package uws.ac.uk.bookmark;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleBooksService {

    private static final String API_KEY = BuildConfig.GOOGLE_BOOKS_API_KEY;

    private static final String TAG = "BOOKMARK_LOADING";
    private static final String CACHE_NAME = "google_books_cache";
    private static final long CACHE_DURATION = 7L * 24 * 60 * 60 * 1000;
    private static final long REQUEST_DELAY = 1500;
    private static final int MAX_ATTEMPTS = 1;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final SharedPreferences cache;

    private long lastRequestTime = 0;

    public GoogleBooksService(Context context) {
        cache = context.getApplicationContext()
                .getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE);
    }

    public interface BooksCallback {
        void onSuccess(ArrayList<Book> books);
        void onError(String message);
    }

    public void getBooks(String query, BooksCallback callback) {
        getBooks(query, 0, 20, "relevance", callback);
    }

    // retrieve additional books using pagination
    public void getBooks(
            String query,
            int startIndex,
            int maxResults,
            String orderBy,
            BooksCallback callback) {

        int safeStart = Math.max(0, startIndex);
        int safeCount = Math.max(1, Math.min(40, maxResults));

        String sort = "newest".equals(orderBy)
                ? "newest" : "relevance";

        String cacheKey = query + "|" + safeStart
                + "|" + safeCount + "|" + sort;

        executor.execute(() -> {

            // check for saved results before making another api request
            String savedResponse = cache.getString(cacheKey, null);
            long savedTime = cache.getLong(cacheKey + "_time", 0);

            if (savedResponse != null
                    && System.currentTimeMillis() - savedTime < CACHE_DURATION) {

                try {

                    ArrayList<Book> books = parseBooks(savedResponse);

                    if (!books.isEmpty()) {
                        Log.d(TAG, "loaded books from cache: " + query);

                        mainHandler.post(() -> callback.onSuccess(books));
                        return;
                    }

                    // remove empty cached results and request fresh books
                    cache.edit()
                            .remove(cacheKey)
                            .remove(cacheKey + "_time")
                            .apply();

                    Log.d(TAG, "removed empty cache: " + query);

                } catch (Exception e) {
                    Log.e(TAG, "could not read saved books: " + query);
                }
            }

            String endpoint;

            try {
                endpoint =
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&startIndex=" + safeStart
                                + "&maxResults=" + safeCount
                                + "&orderBy=" + sort
                                + "&printType=books"
                                + "&key="
                                + URLEncoder.encode(API_KEY, "UTF-8");

            } catch (Exception e) {
                mainHandler.post(() ->
                        callback.onError("could not build request"));
                return;
            }

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

                HttpURLConnection connection = null;

                try {
                    // leave a gap between google books requests
                    long elapsed = System.currentTimeMillis() - lastRequestTime;
                    long waitTime = REQUEST_DELAY - elapsed;

                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }

                    lastRequestTime = System.currentTimeMillis();

                    connection = (HttpURLConnection)
                            new URL(endpoint).openConnection();

                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int status = connection.getResponseCode();

                    if (status == 429) {

                        Log.w(TAG, "google books rate limit: "
                                + query + " | attempt: " + attempt);

                        if (attempt == MAX_ATTEMPTS) {
                            throw new Exception("HTTP error 429");
                        }

                        long retryDelay = 5000L * (1L << (attempt - 1));

                        String retryAfter =
                                connection.getHeaderField("Retry-After");

                        if (retryAfter != null) {
                            try {
                                retryDelay = Math.max(
                                        retryDelay,
                                        Long.parseLong(retryAfter.trim()) * 1000L
                                );
                            } catch (NumberFormatException ignored) {
                                // use the normal retry delay
                            }
                        }

                        connection.disconnect();
                        connection = null;

                        Thread.sleep(Math.min(retryDelay, 60000));
                        continue;
                    }

                    if (status != 200) {
                        throw new Exception("HTTP error " + status);
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(),
                                    "UTF-8"
                            )
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    try {
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    } finally {
                        reader.close();
                    }

                    ArrayList<Book> books = parseBooks(response.toString());

                    // save successful results for future app launches
                    if (!books.isEmpty()) {
                        cache.edit()
                                .putString(cacheKey, response.toString())
                                .putLong(cacheKey + "_time",
                                        System.currentTimeMillis())
                                .apply();
                    }


                    Log.d(TAG, "loaded books from api: " + query);

                    mainHandler.post(() -> callback.onSuccess(books));
                    return;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    mainHandler.post(() ->
                            callback.onError("request interrupted"));
                    return;

                } catch (Exception e) {
                    String message = e.getMessage() == null
                            ? "Unable to load books"
                            : e.getMessage();

                    Log.e(TAG, "google books failed: "
                            + query + " | " + message);

                    mainHandler.post(() -> callback.onError(message));
                    return;

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    private ArrayList<Book> parseBooks(String response) throws Exception {

        JSONObject json = new JSONObject(response);

        JSONArray items = json.optJSONArray("items");
        ArrayList<Book> books = new ArrayList<>();

        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                JSONObject volume =
                        item.optJSONObject("volumeInfo");

                if (volume == null) {
                    continue;
                }

                String id = item.optString("id", "");
                String title = volume.optString("title", "");
                String description =
                        volume.optString("description", "");

                JSONArray authorArray =
                        volume.optJSONArray("authors");

                StringBuilder authors = new StringBuilder();

                if (authorArray != null) {
                    for (int j = 0; j < authorArray.length(); j++) {
                        if (j > 0) {
                            authors.append(", ");
                        }

                        authors.append(
                                authorArray.optString(j)
                        );
                    }
                }

                String coverUrl = "";

                JSONObject images =
                        volume.optJSONObject("imageLinks");

                if (images != null) {
                    coverUrl = images.optString(
                            "thumbnail",
                            images.optString("smallThumbnail", "")
                    );

                    coverUrl = coverUrl.replace(
                            "http://",
                            "https://"
                    );
                }

                // retain publication dates for filtering
                String publishedDate =
                        volume.optString("publishedDate", "");

                if (!publishedDate.isEmpty()) {
                    id = id + "|published:" + publishedDate;
                }

                books.add(new Book(
                        id,
                        title,
                        authors.toString(),
                        description,
                        coverUrl
                ));
            }
        }

        return books;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
