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

public class NYTBooksService {

    private static final String API_KEY = BuildConfig.NYT_API_KEY;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface BooksCallback {
        void onSuccess(ArrayList<Book> books);
        void onError(String message);
    }

    public void getTrendingBooks(BooksCallback callback) {
        getBooksByCategory(
                "combined-print-and-e-book-fiction",
                callback
        );
    }

    // retrieve a selected nyt bestseller category
    public void getBooksByCategory(
            String category,
            BooksCallback callback) {

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                // retrieve the current fiction bestseller list
                String endpoint =
                        "https://api.nytimes.com/svc/books/v3/lists/current/"
                                + URLEncoder.encode(category, "UTF-8")
                                + ".json";

                String urlString = endpoint + "?api-key="
                        + URLEncoder.encode(API_KEY, "UTF-8");

                URL url = new URL(urlString);

                connection = (HttpURLConnection) url.openConnection();
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

                JSONArray results = json
                        .getJSONObject("results")
                        .getJSONArray("books");

                ArrayList<Book> books = new ArrayList<>();

                // convert nyt results into our existing book model
                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.getJSONObject(i);

                    String isbn =
                            item.optString("primary_isbn13");

                    String title = item.optString("title");
                    String author = item.optString("author");
                    String description =
                            item.optString("description");

                    String cover = item.optString("book_image")
                            .replace("http://", "https://");

                    books.add(new Book(
                            isbn,
                            title,
                            author,
                            description,
                            cover
                    ));
                }

                mainHandler.post(
                        () -> callback.onSuccess(books)
                );

            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "Unable to load NYT books"
                        : e.getMessage();

                mainHandler.post(
                        () -> callback.onError(message)
                );

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
