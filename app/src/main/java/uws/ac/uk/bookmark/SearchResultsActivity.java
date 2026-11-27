
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
import android.graphics.Color;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Locale;

public class SearchResultsActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY =
            "uws.ac.uk.bookmark.SEARCH_QUERY";

    private GoogleBooksService googleBooksService;
    private SearchResultsAdapter resultsAdapter;
    private ProgressBar resultsProgress;
    private TextView resultsStatus;
    private View resultsLoadMore;
    private String[][] popularBookSelections;
    private String popularBookSource;
    private final ArrayList<Book> popularBooks = new ArrayList<>();
    private int popularBooksRequested = 0;
    private boolean loadingPopularBooks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // match the android system bars to the bookmark theme
        getWindow().setStatusBarColor(Color.parseColor("#BCA9CC"));
        getWindow().setNavigationBarColor(Color.parseColor("#BCA9CC"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        // use dark status bar icons
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        // keep the search results clear of android system bars
        View root = findViewById(android.R.id.content);

        root.setBackgroundColor(Color.parseColor("#BCA9CC"));

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);


        // Connect Java to the XML layout
        TextView resultsTitle = findViewById(R.id.resultsTitle);
        resultsProgress = findViewById(R.id.resultsProgress);
        resultsStatus = findViewById(R.id.resultsStatus);
        resultsLoadMore = findViewById(R.id.resultsLoadMore);
        resultsLoadMore.setOnClickListener(v -> loadMorePopularBooks());

        RecyclerView resultsRecyclerView =
                findViewById(R.id.resultsRecyclerView);

        // Set up the book results list
        resultsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        resultsAdapter = new SearchResultsAdapter();
        resultsRecyclerView.setAdapter(resultsAdapter);

        // Bottom navigation
        setupNavigation();

        // Check whether a popular list has been selected
        String popularList = getIntent().getStringExtra(
                ExploreActivity.EXTRA_POPULAR_LIST);

        if (popularList != null) {

            googleBooksService = new GoogleBooksService(this);

            loadPopularList(popularList, resultsTitle);

            return;
        }

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
        googleBooksService = new GoogleBooksService(this);

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

    // Load the selected popular book list
    private void loadPopularList(
            String popularList,
            TextView resultsTitle) {

        String[][] selectedBooks;
        String source;

        switch (popularList) {

            case "most_read":

                resultsTitle.setText("most read this month");

                source = "Goodreads - November 2026";

                selectedBooks = new String[][]{
                        {"A Court of Splintered Harmony", "Sarah J. Maas"},
                        {"The Witch", "Freida McFadden"},
                        {"Threshing Day", "Rebecca Yarros"},
                        {"Yesteryear", "Caro Claire Burke"},
                        {"The Calamity Club", "Kathryn Stockett"},
                        {"Project Hail Mary", "Andy Weir"},
                        {"Mad Mabel", "Sally Hepworth"},
                        {"Whistler", "Ann Patchett"},
                        {"Dear Debbie", "Freida McFadden"},
                        {"The Housemaid", "Freida McFadden"}
                };

                break;

            case "booktok":

                resultsTitle.setText("booktok favourites");

                source = "BookTok - bookseller selections";

                selectedBooks = new String[][]{
                        {"Fourth Wing", "Rebecca Yarros"},
                        {"A Court of Thorns and Roses", "Sarah J. Maas"},
                        {"Verity", "Colleen Hoover"},
                        {"The Seven Husbands of Evelyn Hugo", "Taylor Jenkins Reid"},
                        {"The Song of Achilles", "Madeline Miller"},
                        {"Icebreaker", "Hannah Grace"},
                        {"Heated Rivalry", "Rachel Reid"},
                        {"Binding 13", "Chloe Walsh"},
                        {"Alchemised", "SenLinYu"},
                        {"Powerless", "Lauren Roberts"}
                };

                break;

            case "award_winners":

                resultsTitle.setText("award winners");

                source = "Booker Prize winners - 2016 to 2025";

                selectedBooks = new String[][]{
                        {"Flesh", "David Szalay"},
                        {"Orbital", "Samantha Harvey"},
                        {"Prophet Song", "Paul Lynch"},
                        {"The Seven Moons of Maali Almeida", "Shehan Karunatilaka"},
                        {"The Promise", "Damon Galgut"},
                        {"Shuggie Bain", "Douglas Stuart"},
                        {"Girl, Woman, Other", "Bernardine Evaristo"},
                        {"Milkman", "Anna Burns"},
                        {"Lincoln in the Bardo", "George Saunders"},
                        {"The Sellout", "Paul Beatty"}
                };

                break;

            default:

                resultsStatus.setText("Popular list not available");
                return;
        }

        popularBookSelections = selectedBooks;
        popularBookSource = source;
        popularBooks.clear();
        popularBooksRequested = 0;

        loadMorePopularBooks();
    }

    private void loadMorePopularBooks() {

        if (loadingPopularBooks || popularBookSelections == null
                || popularBooksRequested >= popularBookSelections.length) {
            return;
        }

        loadingPopularBooks = true;
        resultsLoadMore.setVisibility(View.GONE);
        resultsProgress.setVisibility(View.VISIBLE);

        resultsStatus.setText(
                (popularBooksRequested == 0
                        ? "Loading books...\n"
                        : "Loading more books...\n")
                        + popularBookSource);

        int endIndex = Math.min(
                popularBooksRequested + 5,
                popularBookSelections.length
        );

        loadPopularBook(
                popularBookSelections,
                popularBooksRequested,
                endIndex,
                popularBooks,
                popularBookSource
        );
    }

    // Retrieve each selected book using Google Books
    private void loadPopularBook(
            String[][] selectedBooks,
            int index,
            int endIndex,
            ArrayList<Book> popularBooks,
            String source) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        // Check whether all selected books have been requested
        if (index >= endIndex) {

            popularBooksRequested = endIndex;
            loadingPopularBooks = false;
            resultsProgress.setVisibility(View.GONE);
            resultsLoadMore.setVisibility(
                    popularBooksRequested < selectedBooks.length
                            ? View.VISIBLE : View.GONE);

            if (popularBooks.isEmpty()) {

                resultsStatus.setText(
                        "Could not load books\n" + source);

            } else {

                resultsStatus.setText(
                        "Books found: " + popularBooks.size()
                                + " of " + popularBooksRequested
                                + "\n" + source);
            }

            return;
        }

        String title = selectedBooks[index][0];
        String author = selectedBooks[index][1];

        String query = "intitle:\"" + title + "\""
                + " inauthor:\"" + author + "\"";

        googleBooksService.getBooks(
                query,
                0,
                10,
                "relevance",
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // Find the matching book and author
                        for (Book book : books) {

                            if (normalise(book.getTitle())
                                    .equals(normalise(title))
                                    && normalise(book.getAuthors())
                                    .contains(normalise(author))
                                    && book.getId() != null
                                    && !book.getId().trim().isEmpty()) {

                                popularBooks.add(book);

                                break;
                            }
                        }

                        // Display the retrieved books
                        resultsAdapter.setBooks(popularBooks);

                        // Retrieve the next book in the list
                        loadPopularBook(
                                selectedBooks,
                                index + 1,
                                endIndex,
                                popularBooks,
                                source
                        );
                    }

                    @Override
                    public void onError(String message) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // Continue loading the remaining books
                        loadPopularBook(
                                selectedBooks,
                                index + 1,
                                endIndex,
                                popularBooks,
                                source
                        );
                    }
                }
        );
    }

    // Normalise titles and authors for matching
    private String normalise(String value) {

        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
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

    // Refresh the saved bookmarks when returning to search results
    @Override
    protected void onResume() {
        super.onResume();

        if (resultsAdapter != null) {
            resultsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (googleBooksService != null) {
            googleBooksService.shutdown();
        }
    }
}