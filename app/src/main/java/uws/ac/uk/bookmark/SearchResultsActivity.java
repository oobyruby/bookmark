
package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SearchResultsActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY =
            "uws.ac.uk.bookmark.SEARCH_QUERY";

    private GoogleBooksService googleBooksService;
    private SearchResultsAdapter resultsAdapter;
    private ProgressBar resultsProgress;
    private TextView resultsStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Connect Java to the XML layout
        TextView resultsTitle = findViewById(R.id.resultsTitle);
        resultsProgress = findViewById(R.id.resultsProgress);
        resultsStatus = findViewById(R.id.resultsStatus);

        RecyclerView resultsRecyclerView =
                findViewById(R.id.resultsRecyclerView);

        // Set up the book results list
        resultsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        resultsAdapter = new SearchResultsAdapter();
        resultsRecyclerView.setAdapter(resultsAdapter);

        // Bottom navigation
        setupNavigation();

        // Get the search entered on the Explore page
        String query = getIntent().getStringExtra(EXTRA_QUERY);

        if (query == null || query.trim().isEmpty()) {
            resultsTitle.setText(R.string.search_results_title);
            resultsStatus.setText(R.string.search_results_no_query);
            return;
        }

        query = query.trim();

        resultsTitle.setText(
                getString(R.string.search_results_for, query));

        resultsStatus.setText(R.string.search_results_loading);
        resultsProgress.setVisibility(View.VISIBLE);

        // Request books from the existing Google Books API
        googleBooksService = new GoogleBooksService();

        googleBooksService.getBooks(
                query,
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        resultsProgress.setVisibility(View.GONE);

                        // Display books using our adapter
                        resultsAdapter.setBooks(books);

                        if (books.isEmpty()) {
                            resultsStatus.setText(
                                    R.string.search_results_empty);
                        } else {
                            resultsStatus.setText(
                                    getString(
                                            R.string.search_results_count,
                                            books.size()
                                    )
                            );
                        }
                    }

                    @Override
                    public void onError(String message) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        resultsProgress.setVisibility(View.GONE);
                        resultsStatus.setText(
                                getString(
                                        R.string.search_results_error,
                                        message
                                )
                        );
                    }
                }
        );
    }

    // Set up the existing bottom navigation
    private void setupNavigation() {

        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SearchResultsActivity.this,
                    MainActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        navExplore.setOnClickListener(v -> {
            // Return to the Explore page
            finish();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SearchResultsActivity.this,
                    ProfileActivity.class);

            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (googleBooksService != null) {
            googleBooksService.shutdown();
        }
    }
}
