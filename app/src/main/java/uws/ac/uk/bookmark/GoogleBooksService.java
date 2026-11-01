package uws.ac.uk.bookmark;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleBooksService {

    public interface BooksCallback {
        void onSuccess(ArrayList<Book> books);
        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void getBooks(String query, BooksCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                URL url = new URL(
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + encodedQuery
                                + "&maxResults=20"
                                + "&key=AIzaSyC6mqqSLsu6OIJbT293uRgOErtJiGnzQS0"
                );
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Google Books returned HTTP " + responseCode);
                }

                StringBuilder response = new StringBuilder();

                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(input, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray items = json.optJSONArray("items");
                ArrayList<Book> books = new ArrayList<>();

                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;

                        JSONObject volume = item.optJSONObject("volumeInfo");
                        if (volume == null) continue;

                        String id = item.optString("id", "");
                        String title = volume.optString("title", "Untitled");
                        String description = volume.optString("description", "");

                        JSONArray authorArray = volume.optJSONArray("authors");
                        ArrayList<String> authors = new ArrayList<>();

                        if (authorArray != null) {
                            for (int j = 0; j < authorArray.length(); j++) {
                                authors.add(authorArray.optString(j));
                            }
                        }

                        String coverUrl = "";

                        JSONObject imageLinks = volume.optJSONObject("imageLinks");
                        if (imageLinks != null) {
                            coverUrl = imageLinks.optString("thumbnail", "");
                            coverUrl = coverUrl.replace("http://", "https://");
                        }

                        books.add(new Book(
                                id,
                                title,
                                String.join(", ", authors),
                                description,
                                coverUrl
                        ));
                    }
                }

                mainHandler.post(() -> callback.onSuccess(books));

            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "Unable to load books."
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
