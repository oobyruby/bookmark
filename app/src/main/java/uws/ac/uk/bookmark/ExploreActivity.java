package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ExploreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        // Search bar
        EditText searchBar = findViewById(R.id.searchBar);

        // Bottom navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // See All genres button
        TextView seeAllGenres = findViewById(R.id.seeAllGenres);

        // Genre tile references
        View genreRomance = findViewById(R.id.genreRomance);
        View genreFantasy = findViewById(R.id.genreFantasy);
        View genreThriller = findViewById(R.id.genreThriller);
        View genreYoungAdult = findViewById(R.id.genreYoungAdult);
        View genreNonFiction = findViewById(R.id.genreNonFiction);
        View genreContemporary = findViewById(R.id.genreContemporary);
        View genreHorror = findViewById(R.id.genreHorror);
        View genreScience = findViewById(R.id.genreScience);
        View genreComics = findViewById(R.id.genreComics);

        // Handle the Search button on the keyboard
        searchBar.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                String query = searchBar.getText().toString().trim();

                if (query.isEmpty()) {
                    Toast.makeText(
                            ExploreActivity.this,
                            getString(R.string.search_empty_message),
                            Toast.LENGTH_SHORT
                    ).show();

                } else {
                    Toast.makeText(
                            ExploreActivity.this,
                            getString(R.string.searching_for_message, query),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Open Search Results and pass the search query
                    Intent intent = new Intent(
                            ExploreActivity.this,
                            SearchResultsActivity.class
                    );

                    intent.putExtra(
                            SearchResultsActivity.EXTRA_QUERY,
                            query
                    );

                    startActivity(intent);
                }

                return true;
            }

            return false;
        });

        // Popular Lists are currently display-only
        // Functionality will be added when the reading lists are configured in Firebase

        // See All button displays the available genres
        seeAllGenres.setOnClickListener(v -> {

            String[] genres = {
                    "Romance",
                    "Fantasy",
                    "Thriller",
                    "Young Adult",
                    "Nonfiction",
                    "Contemporary",
                    "Horror",
                    "Science",
                    "Comics"
            };

            new AlertDialog.Builder(ExploreActivity.this)
                    .setTitle(R.string.explore_all_genres_title)
                    .setItems(genres, (dialog, which) -> {

                        // Open Search Results for the selected genre
                        openGenreSearch(genres[which]);

                    })
                    .setNegativeButton(R.string.explore_cancel, null)
                    .show();
        });

        // Genre tile click listeners
        // Opens Search Results using the selected Google Books subject

        genreRomance.setOnClickListener(v -> {
            openGenreSearch("Romance");
        });

        genreFantasy.setOnClickListener(v -> {
            openGenreSearch("Fantasy");
        });

        genreThriller.setOnClickListener(v -> {
            openGenreSearch("Thriller");
        });

        genreYoungAdult.setOnClickListener(v -> {
            openGenreSearch("Young Adult");
        });

        genreNonFiction.setOnClickListener(v -> {
            openGenreSearch("Nonfiction");
        });

        genreContemporary.setOnClickListener(v -> {
            openGenreSearch("Contemporary");
        });

        genreHorror.setOnClickListener(v -> {
            openGenreSearch("Horror");
        });

        genreScience.setOnClickListener(v -> {
            openGenreSearch("Science");
        });

        genreComics.setOnClickListener(v -> {
            openGenreSearch("Comics");
        });

        // Home navigation
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ExploreActivity.this,
                    MainActivity.class
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Explore navigation
        navExplore.setOnClickListener(v -> {
            // Explore is already open
        });

        // Open Nearby Libraries from Explore
        navMap.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ExploreActivity.this,
                    NearbyLibrariesActivity.class
            );
            startActivity(intent);
        });

        // Profile navigation
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ExploreActivity.this,
                    ProfileActivity.class
            );
            startActivity(intent);
        });
    }

    // Search for books using the selected genre
    private void openGenreSearch(String genre) {

        // Google Books uses subject: to search by category
        String query = "subject:" + genre;

        // Open Search Results
        Intent intent = new Intent(
                ExploreActivity.this,
                SearchResultsActivity.class
        );

        // Pass the genre query to Search Results
        intent.putExtra(
                SearchResultsActivity.EXTRA_QUERY,
                query
        );

        startActivity(intent);
    }
}