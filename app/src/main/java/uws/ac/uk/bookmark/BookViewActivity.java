package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Locale;

public class BookViewActivity extends AppCompatActivity {

    // Information passed from the Search Results screen
    public static final String EXTRA_TITLE = "book_title";
    public static final String EXTRA_AUTHORS = "book_authors";
    public static final String EXTRA_COVER_URL = "book_cover_url";
    public static final String EXTRA_DESCRIPTION = "book_description";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_view);

        // Connect Java to the XML layout
        ImageView bookCover = findViewById(R.id.bookCover);
        TextView bookTitle = findViewById(R.id.bookTitle);
        TextView bookAuthors = findViewById(R.id.bookAuthors);
        TextView bookDescription = findViewById(R.id.bookDescription);
        TextView readMoreButton = findViewById(R.id.readMoreButton);
        TextView bookRating = findViewById(R.id.bookRating);
        TextView backButton = findViewById(R.id.backButton);

        // Get the selected book's information
        Intent intent = getIntent();

        String title = intent.getStringExtra(EXTRA_TITLE);
        String authors = intent.getStringExtra(EXTRA_AUTHORS);
        String coverUrl = intent.getStringExtra(EXTRA_COVER_URL);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);

        // Display the book title
        if (title != null && !title.isEmpty()) {
            bookTitle.setText(title.toLowerCase(Locale.ROOT));
        } else {
            bookTitle.setText(R.string.book_title_not_available);
        }

        // Display the author
        if (authors != null && !authors.isEmpty()) {
            bookAuthors.setText(authors.toLowerCase(Locale.ROOT));
        } else {
            bookAuthors.setText(R.string.author_not_available);
        }

        // Display the book cover using Glide
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(bookCover);
        } else {
            bookCover.setImageResource(
                    R.drawable.genre_tile_background
            );
        }


        // Display the book description
        if (description != null && !description.isEmpty()) {
            bookDescription.setText(
                    Html.fromHtml(
                            description,
                            Html.FROM_HTML_MODE_COMPACT
                    )
            );
        } else {
            bookDescription.setText(
                    R.string.book_description_not_available
            );
        }

        // expand or collapse longer descriptions
        bookDescription.setMaxLines(6);
        bookDescription.setEllipsize(TextUtils.TruncateAt.END);

        bookDescription.post(() -> {
            if (bookDescription.getLayout() == null) {
                return;
            }

            int lastVisibleLine = bookDescription.getLineCount() - 1;

            boolean isTruncated = lastVisibleLine >= 0
                    && bookDescription.getLayout()
                    .getEllipsisCount(lastVisibleLine) > 0;

            readMoreButton.setVisibility(
                    isTruncated ? View.VISIBLE : View.GONE
            );
        });

        readMoreButton.setOnClickListener(v -> {
            boolean isExpanded =
                    "read less ‹".contentEquals(readMoreButton.getText());

            if (isExpanded) {
                bookDescription.setMaxLines(6);
                bookDescription.setEllipsize(TextUtils.TruncateAt.END);
                readMoreButton.setText("read more ›");
            } else {
                bookDescription.setMaxLines(Integer.MAX_VALUE);
                bookDescription.setEllipsize(null);
                readMoreButton.setText("read less ‹");
            }
        });

        // return to the previous screen
        backButton.setOnClickListener(v -> {
            finish();
        });

        // retrieve the book rating from google books
        loadBookRating(title, authors, bookRating);

        // Bottom navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // Home navigation
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(
                    BookViewActivity.this,
                    MainActivity.class
            );
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        });

        // Explore navigation
        navExplore.setOnClickListener(v -> {
            Intent exploreIntent = new Intent(
                    BookViewActivity.this,
                    ExploreActivity.class
            );
            exploreIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(exploreIntent);
        });

        // Profile navigation
        navProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(
                    BookViewActivity.this,
                    ProfileActivity.class
            );
            startActivity(profileIntent);
        });
    }

    // retrieve ratings from the google books api
    private void loadBookRating(
            String title,
            String authors,
            TextView ratingView) {

        if (title == null || title.trim().isEmpty()) {
            ratingView.setText("not yet rated");
            return;
        }

        new Thread(() -> {

            HttpURLConnection connection = null;
            String ratingText = "not yet rated";

            try {

                // search for the selected book by title and author
                String query = "intitle:\"" + title + "\"";

                if (authors != null && !authors.trim().isEmpty()) {
                    String firstAuthor = authors.split(",")[0].trim();
                    query += " inauthor:\"" + firstAuthor + "\"";
                }

                String endpoint =
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&maxResults=20";

                connection = (HttpURLConnection)
                        new URL(endpoint).openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == 200) {

                    // read the api response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    // extract the book results
                    JSONObject json =
                            new JSONObject(response.toString());

                    JSONArray items = json.optJSONArray("items");

                    if (items != null) {

                        double bestRating = -1;
                        int bestCount = -1;

                        // find the matching edition with the most ratings
                        for (int i = 0; i < items.length(); i++) {

                            JSONObject volumeInfo = items
                                    .getJSONObject(i)
                                    .optJSONObject("volumeInfo");

                            if (volumeInfo == null
                                    || !volumeInfo.has("averageRating")) {
                                continue;
                            }

                            String resultTitle =
                                    volumeInfo.optString("title", "");

                            if (!normalise(resultTitle)
                                    .equals(normalise(title))) {
                                continue;
                            }

                            double rating = volumeInfo.optDouble(
                                    "averageRating", -1
                            );

                            int count = volumeInfo.optInt(
                                    "ratingsCount", 0
                            );

                            if (rating >= 0 && rating <= 5
                                    && count > bestCount) {
                                bestRating = rating;
                                bestCount = count;
                            }
                        }

                        // format the rating for display
                        if (bestRating >= 0) {
                            ratingText = String.format(
                                    Locale.UK,
                                    "★ %.1f / 5  (%d ratings)",
                                    bestRating,
                                    Math.max(bestCount, 0)
                            );
                        }
                    }
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "BookViewActivity",
                        "Could not load rating",
                        e
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // update the rating on the main thread
            String finalRatingText = ratingText;

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    ratingView.setText(finalRatingText);
                }
            });

        }).start();
    }

    // normalise book titles for matching api results
    private String normalise(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
