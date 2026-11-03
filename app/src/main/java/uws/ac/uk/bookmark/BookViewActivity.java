
package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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

        // Get the selected book's information
        Intent intent = getIntent();

        String title = intent.getStringExtra(EXTRA_TITLE);
        String authors = intent.getStringExtra(EXTRA_AUTHORS);
        String coverUrl = intent.getStringExtra(EXTRA_COVER_URL);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);

        // Display the book title
        if (title != null && !title.isEmpty()) {
            bookTitle.setText(title);
        } else {
            bookTitle.setText(R.string.book_title_not_available);
        }

        // Display the author
        if (authors != null && !authors.isEmpty()) {
            bookAuthors.setText(authors);
        } else {
            bookAuthors.setText(R.string.author_not_available);
        }

        // Display the book cover using Glide
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .centerInside()
                    .into(bookCover);
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
}