package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BookListActivity extends AppCompatActivity {

    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_BOOKS = "books";

    // number of books requested per api call
    private static final int PAGE_SIZE = 40;

    // nyt bestseller categories used for additional trending books
    private static final String[] TRENDING_CATEGORIES = {
            "hardcover-fiction",
            "trade-fiction-paperback",
            "young-adult-hardcover",
            "series-books"
    };

    // google books categories used for new releases
    private static final String[] RELEASE_QUERIES = {
            "subject:fiction",
            "subject:romance",
            "subject:fantasy",
            "subject:mystery"
    };

    // google books categories used for all-time favourites
    private static final String[] FAVOURITE_QUERIES = {
            "subject:fiction",
            "subject:fantasy",
            "subject:romance",
            "subject:classics",
            "subject:young-adult-fiction",
            "subject:thrillers"
    };

    private GoogleBooksService googleBooksService;
    private NYTBooksService nytBooksService;
    private SearchResultsAdapter adapter;

    // track displayed books to prevent duplicates
    private final ArrayList<Book> displayedBooks = new ArrayList<>();
    private final Set<String> displayedKeys = new HashSet<>();

    private TextView loadMoreButton;
    private String section;

    // track pagination and loading status
    private int categoryIndex = 0;
    private int startIndex = 0;
    private boolean loading = false;
    private boolean exhausted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        TextView title = findViewById(R.id.bookListTitle);
        TextView backButton = findViewById(R.id.backButton);
        loadMoreButton = findViewById(R.id.loadMoreButton);

        RecyclerView recyclerView =
                findViewById(R.id.bookListRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // get the selected section and books from the homepage
        section = getIntent().getStringExtra(EXTRA_SECTION);
        String booksJson = getIntent().getStringExtra(EXTRA_BOOKS);

        title.setText(section == null ? "books" : section);

        // display the selected section's books
        ArrayList<Book> books = parseBooks(booksJson);

        adapter = new SearchResultsAdapter();
        recyclerView.setAdapter(adapter);

        addBooks(books);

        // initialise the book api services
        googleBooksService = new GoogleBooksService();
        nytBooksService = new NYTBooksService();

        // load additional books when the button is selected
        loadMoreButton.setOnClickListener(v -> loadMoreBooks());

        backButton.setOnClickListener(v -> finish());

        setupNavigation();

        // only show load more for supported book sections
        if (!"trending books".equals(section)
                && !"new releases".equals(section)
                && !"all-time favourites".equals(section)) {
            loadMoreButton.setVisibility(View.GONE);
        }
    }

    // determine which api to use for the selected section
    private void loadMoreBooks() {
        if (loading || exhausted) {
            return;
        }

        if ("trending books".equals(section)) {
            loadMoreTrending();
        } else if ("new releases".equals(section)
                || "all-time favourites".equals(section)) {
            loadMoreGoogleBooks();
        }
    }

    // retrieve the next nyt bestseller category
    private void loadMoreTrending() {
        if (categoryIndex >= TRENDING_CATEGORIES.length) {
            showEndOfResults();
            return;
        }

        setLoading(true);

        String category = TRENDING_CATEGORIES[categoryIndex];

        nytBooksService.getBooksByCategory(
                category,
                new NYTBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        categoryIndex++;

                        // add results without repeating existing books
                        int added = addBooks(books);

                        if (categoryIndex >= TRENDING_CATEGORIES.length) {
                            showEndOfResults();
                        } else {
                            setLoading(false);
                        }

                        showAddedCount(added);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadError(message);
                    }
                }
        );
    }

    // retrieve the next page of google books results
    private void loadMoreGoogleBooks() {
        String[] queries = "new releases".equals(section)
                ? RELEASE_QUERIES
                : FAVOURITE_QUERIES;

        if (categoryIndex >= queries.length) {
            showEndOfResults();
            return;
        }

        setLoading(true);

        String query = queries[categoryIndex];

        // sort new releases by date and favourites by relevance
        String orderBy = "new releases".equals(section)
                ? "newest"
                : "relevance";

        int requestedStart = startIndex;

        googleBooksService.getBooks(
                query,
                requestedStart,
                PAGE_SIZE,
                orderBy,
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // remove unsuitable books from the results
                        ArrayList<Book> filtered = new ArrayList<>();

                        for (Book book : books) {
                            if (isSuitableBook(book)) {
                                filtered.add(book);
                            }
                        }

                        int added = addBooks(filtered);

                        // move to the next page or search category
                        if (books.size() < PAGE_SIZE) {
                            categoryIndex++;
                            startIndex = 0;
                        } else {
                            startIndex += PAGE_SIZE;

                            // limit each category to five pages
                            if (startIndex >= 200) {
                                categoryIndex++;
                                startIndex = 0;
                            }
                        }

                        if (categoryIndex >= queries.length) {
                            showEndOfResults();
                        } else {
                            setLoading(false);
                        }

                        showAddedCount(added);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showLoadError(message);
                    }
                }
        );
    }

    // filter books with missing information or unsuitable titles
    private boolean isSuitableBook(Book book) {
        if (book == null
                || book.getTitle() == null
                || book.getTitle().trim().isEmpty()
                || book.getAuthors() == null
                || book.getAuthors().trim().isEmpty()
                || book.getCoverUrl() == null
                || book.getCoverUrl().trim().isEmpty()) {
            return false;
        }

        String lowerTitle =
                book.getTitle().toLowerCase(Locale.ROOT);

        // exclude summaries, study guides and workbooks
        if (lowerTitle.contains("summary of")
                || lowerTitle.contains("study guide")
                || lowerTitle.contains("workbook")
                || lowerTitle.contains("analysis of")
                || lowerTitle.contains("companion guide")) {
            return false;
        }

        // only include recent publication years for new releases
        if ("new releases".equals(section)) {
            String id = book.getId();
            String marker = "|published:";

            if (id == null || !id.contains(marker)) {
                return false;
            }

            String published = id.substring(
                    id.indexOf(marker) + marker.length()
            );

            if (published.length() < 4) {
                return false;
            }

            try {
                int year = Integer.parseInt(
                        published.substring(0, 4)
                );

                return year >= 2025 && year <= 2026;

            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    // add new books without displaying duplicates
    private int addBooks(ArrayList<Book> books) {
        int added = 0;

        for (Book book : books) {
            if (book == null
                    || book.getTitle() == null
                    || book.getTitle().trim().isEmpty()) {
                continue;
            }

            // compare normalised book titles and authors
            String key = normalise(book.getTitle())
                    + "|"
                    + normalise(book.getAuthors());

            if (displayedKeys.add(key)) {
                displayedBooks.add(book);
                added++;
            }
        }

        if (added > 0) {
            adapter.setBooks(new ArrayList<>(displayedBooks));
        }

        return added;
    }

    // normalise titles and authors for duplicate checking
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
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    // update the load more button while fetching books
    private void setLoading(boolean isLoading) {
        loading = isLoading;

        loadMoreButton.setEnabled(!isLoading);
        loadMoreButton.setAlpha(isLoading ? 0.6f : 1f);

        loadMoreButton.setText(
                isLoading ? "loading books..." : "load more ↓"
        );
    }

    // hide the button when no more results are available
    private void showEndOfResults() {
        loading = false;
        exhausted = true;

        loadMoreButton.setVisibility(View.GONE);
    }

    // allow the user to retry a failed request
    private void showLoadError(String message) {
        setLoading(false);

        loadMoreButton.setText("retry loading");

        Toast.makeText(
                this,
                "Could not load books: " + message,
                Toast.LENGTH_LONG
        ).show();
    }

    // display the number of additional books loaded
    private void showAddedCount(int added) {
        Toast.makeText(
                this,
                added > 0
                        ? added + " more books loaded"
                        : "No new matching books in this batch",
                Toast.LENGTH_SHORT
        ).show();
    }

    // convert the books passed from the homepage into book objects
    private ArrayList<Book> parseBooks(String booksJson) {
        ArrayList<Book> books = new ArrayList<>();

        if (booksJson == null) {
            return books;
        }

        try {
            JSONArray array = new JSONArray(booksJson);

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                books.add(new Book(
                        item.optString("id"),
                        item.optString("title"),
                        item.optString("authors"),
                        item.optString("description"),
                        item.optString("coverUrl")
                ));
            }

        } catch (Exception e) {
            android.util.Log.e(
                    "BookListActivity",
                    "Could not read book list",
                    e
            );
        }

        return books;
    }

    // connect the bottom navigation buttons
    private void setupNavigation() {
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        navExplore.setOnClickListener(v ->
                startActivity(new Intent(this, ExploreActivity.class)));

        navMap.setOnClickListener(v -> {
            // connect when map screen exists
        });

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    // shut down api services when leaving the screen
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (googleBooksService != null) {
            googleBooksService.shutdown();
        }

        if (nytBooksService != null) {
            nytBooksService.shutdown();
        }
    }
}
