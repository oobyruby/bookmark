package uws.ac.uk.bookmark;

import android.os.Handler;
import android.os.Looper;

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

    private final ExecutorService executor =
            Executors.newFixedThreadPool(3);

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

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

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                int safeStart = Math.max(0, startIndex);
                int safeCount = Math.max(1, Math.min(40, maxResults));

                String sort = "newest".equals(orderBy)
                        ? "newest" : "relevance";

                String endpoint =
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&startIndex=" + safeStart
                                + "&maxResults=" + safeCount
                                + "&orderBy=" + sort
                                + "&printType=books"
                                + "&key="
                                + URLEncoder.encode(API_KEY, "UTF-8");

                connection = (HttpURLConnection)
                        new URL(endpoint).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int status = connection.getResponseCode();

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

                JSONObject json =
                        new JSONObject(response.toString());

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

                mainHandler.post(() -> callback.onSuccess(books));

            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "Unable to load books"
                        : e.getMessage();

                mainHandler.post(() -> callback.onError(message));

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
