package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import java.util.IdentityHashMap;
import java.util.Map;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private NYTBooksService nytBooksService;
    private GoogleBooksService googleBooksService;
    private LatestReviewsFeed latestReviewsFeed;


    // ambient light sensor and theme settings
    private AmbientLightHelper ambientLightHelper;
    private boolean darkMode = false;

    private final Map<TextView, ColorStateList> originalTextColours =
            new IdentityHashMap<>();

    private final Map<View, Drawable> originalBackgrounds =
            new IdentityHashMap<>();

    private static final int DARK_BACKGROUND =
            Color.rgb(30, 26, 35);

    private static final int DARK_PANEL =
            Color.rgb(48, 41, 56);

    private static final int DARK_TEXT =
            Color.rgb(240, 231, 246);

    private static final int DARK_SECONDARY_TEXT =
            Color.rgb(196, 181, 207);


    private final ArrayList<Book> trendingBooks = new ArrayList<>();
    private final ArrayList<Book> newReleaseBooks = new ArrayList<>();
    private final ArrayList<Book> stephenKingBooks = new ArrayList<>();
    private final ArrayList<Book> favouriteBooks = new ArrayList<>();

    // track when each homepage collection is ready to display
    private boolean trendingReady = false;
    private boolean newReleasesReady = false;
    private boolean stephenKingReady = false;
    private boolean favouritesReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // keep the status bar lavender and the content below the camera
        getWindow().setStatusBarColor(
                android.graphics.Color.parseColor("#BCA9CC")
        );

        getWindow().setNavigationBarColor(
                android.graphics.Color.parseColor("#BCA9CC")
        );

        getWindow().setNavigationBarContrastEnforced(false);


        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        View root = findViewById(android.R.id.content);

        // colour the area behind the system navigation bar
        root.setBackgroundColor(
                android.graphics.Color.parseColor("#BCA9CC")
        );


        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(
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

        // prevent slow api requests from blocking the homepage
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> {

                    if (!isFinishing() && !isDestroyed()) {
                        findViewById(R.id.homeLoadingOverlay)
                                .setVisibility(View.GONE);
                    }

                }, 8000);

        googleBooksService = new GoogleBooksService(this);

        setupNavigation();
        setupHomeSearch();
        setupSeeAllButtons();
        setupTrendingBooks();
        setupNewReleases();
        setupStephenKingCollection();
        setupAllTimeFavourites();
        setupLatestReviews();
        setupAmbientLightSensor();
    }

    // initialise the latest reviews section
    private void setupLatestReviews() {

        LinearLayout reviewsContainer =
                findViewById(R.id.homeReviewsContainer);

        latestReviewsFeed = new LatestReviewsFeed(
                this,
                reviewsContainer
        );
    }

    // retrieve stephen king books separately from the other collections
    private void loadStephenKingBooks(
            RecyclerView recyclerView,
            String[][] selections,
            int index) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        // stop once all selected books have been checked
        if (index >= selections.length) {

            // mark stephen king as ready when all searches have finished
            markCollectionReady(stephenKingBooks);
            return;
        }

        String title = selections[index][0];
        String author = selections[index][1];

        // search google books using the selected title and author
        String query = "intitle:\"" + title
                + "\" inauthor:\"" + author + "\"";

        android.util.Log.d("BOOKMARK_LOADING",
                "searching for stephen king book: " + title);

        googleBooksService.getBooks(
                query,
                0,
                40,
                "relevance",
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> results) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Book selectedBook = null;

                        // find an exact title match with a valid author and cover
                        for (Book book : results) {

                            if (matchesBook(book, title, author)
                                    && normalise(book.getTitle())
                                    .equals(normalise(title))) {

                                selectedBook = book;
                                break;
                            }
                        }

                        // add the selected book to the collection
                        if (selectedBook != null) {

                            stephenKingBooks.add(selectedBook);

                            recyclerView.setAdapter(
                                    new BookAdapter(
                                            new ArrayList<>(stephenKingBooks)
                                    )
                            );

                            android.util.Log.d("BOOKMARK_LOADING",
                                    "loaded stephen king book: " + title);

                        } else {
                            android.util.Log.w("BOOKMARK_LOADING",
                                    "no matching stephen king book: " + title);
                        }

                        // continue loading the remaining books in order
                        loadStephenKingBooks(
                                recyclerView,
                                selections,
                                index + 1
                        );
                    }

                    @Override
                    public void onError(String message) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        android.util.Log.e("BOOKMARK_LOADING",
                                "stephen king search failed: "
                                        + title + " | error: " + message);

                        // continue loading if an api request fails
                        loadStephenKingBooks(
                                recyclerView,
                                selections,
                                index + 1
                        );
                    }
                }
        );
    }

    // check whether all homepage collections are ready
    private void checkHomeLoadingComplete() {

        android.util.Log.d("BOOKMARK_LOADING",
                "trending: " + trendingReady
                        + " | new releases: " + newReleasesReady
                        + " | stephen king: " + stephenKingReady
                        + " | favourites: " + favouritesReady);

        // show the homepage while other collections continue loading
        if (trendingReady || newReleasesReady
                || stephenKingReady || favouritesReady) {

            findViewById(R.id.homeLoadingOverlay)
                    .setVisibility(View.GONE);
        }
    }

    // mark a collection as ready after its first book or final request
    private void markCollectionReady(ArrayList<Book> books) {

        if (books == trendingBooks) {
            trendingReady = true;

        } else if (books == newReleaseBooks) {
            newReleasesReady = true;

        } else if (books == stephenKingBooks) {
            stephenKingReady = true;

        } else if (books == favouriteBooks) {
            favouritesReady = true;
        }

        checkHomeLoadingComplete();
    }

    // handle book searches from the home page
    private void setupHomeSearch() {

        EditText searchBar = findViewById(R.id.searchBar);

        // handle the search button on the keyboard
        searchBar.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                String query = searchBar.getText().toString().trim();

                if (query.isEmpty()) {

                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.search_empty_message),
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.searching_for_message, query),
                            Toast.LENGTH_SHORT
                    ).show();

                    // open search results and pass the search query
                    Intent intent = new Intent(
                            MainActivity.this,
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
    }

    private void setupNavigation() {
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            // homepage is already open
        });

        navExplore.setOnClickListener(v ->
                startActivity(new Intent(this, ExploreActivity.class)));

        navMap.setOnClickListener(v -> {
            // opens map screen
            startActivity(new Intent(this, NearbyLibrariesActivity.class));
        });

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void setupSeeAllButtons() {

        findViewById(R.id.seeAllTrending).setOnClickListener(v ->
                openBookList("trending books", trendingBooks));

        findViewById(R.id.seeAllNewReleases).setOnClickListener(v ->
                openBookList("new releases", newReleaseBooks));

        findViewById(R.id.seeAllStephenKing).setOnClickListener(v ->
                openBookList("stephen king collection", stephenKingBooks));

        findViewById(R.id.seeAllFavourites).setOnClickListener(v ->
                openBookList("all-time favourites", favouriteBooks));

        findViewById(R.id.seeAllReviews).setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        LatestReviewsActivity.class
                )));
    }

    private void openBookList(String section, ArrayList<Book> books) {
        if (books.isEmpty()) {
            Toast.makeText(this,
                    "Books are still loading",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray array = new JSONArray();

        try {
            for (Book book : books) {
                JSONObject item = new JSONObject();

                item.put("id", book.getId());
                item.put("title", book.getTitle());
                item.put("authors", book.getAuthors());
                item.put("description", book.getDescription());
                item.put("coverUrl", book.getCoverUrl());

                array.put(item);
            }

            Intent intent = new Intent(this, BookListActivity.class);
            intent.putExtra(BookListActivity.EXTRA_SECTION, section);
            intent.putExtra(BookListActivity.EXTRA_BOOKS, array.toString());

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Could not open book list",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTrendingBooks() {
        RecyclerView recyclerView =
                findViewById(R.id.trendingBooksRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        nytBooksService = new NYTBooksService();

        // retrieve current nyt fiction bestsellers
        nytBooksService.getTrendingBooks(
                new NYTBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        trendingBooks.clear();
                        trendingBooks.addAll(books);

                        recyclerView.setAdapter(
                                new BookAdapter(new ArrayList<>(trendingBooks)));

                        // mark trending books as ready after the api responds
                        markCollectionReady(trendingBooks);

                        android.util.Log.d("BOOKMARK_LOADING",
                                "trending books loaded: " + books.size());

                        if (books.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "No trending books found.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }


                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // log the error to identify why trending books failed
                        android.util.Log.e("BOOKMARK_LOADING",
                                "trending books failed: " + message);

                        Toast.makeText(MainActivity.this,
                                "Could not load trending books: " + message,
                                Toast.LENGTH_LONG).show();

                        // mark trending as finished even if the api request fails
                        markCollectionReady(trendingBooks);
                    }
                });
    }


    private void setupNewReleases() {
        RecyclerView recyclerView =
                findViewById(R.id.newReleasesRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        // retrieve newer fiction books using one google books search
        nytBooksService.getNewReleaseBooks(
                new NYTBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> results) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // log the number of books returned by google books
                        android.util.Log.d("BOOKMARK_LOADING",
                                "new releases api returned: " + results.size());

                        // add books with covers and avoid duplicates
                        addCollectionBooks(results, newReleaseBooks, 15);

                        // log the number of books remaining after filtering
                        android.util.Log.d("BOOKMARK_LOADING",
                                "new releases after filtering: " + newReleaseBooks.size());

                        recyclerView.setAdapter(
                                new BookAdapter(
                                        new ArrayList<>(newReleaseBooks)));

                        android.util.Log.d("BOOKMARK_LOADING",
                                "new releases loaded: " + newReleaseBooks.size());

                        markCollectionReady(newReleaseBooks);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        android.util.Log.e("BOOKMARK_LOADING",
                                "new releases failed: " + message);

                        markCollectionReady(newReleaseBooks);
                    }
                });
    }

    // retrieve the stephen king collection using the google books api
    private void setupStephenKingCollection() {

        RecyclerView recyclerView =
                findViewById(R.id.stephenKingRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        // the first five books are ordered for the audio preview feature
        String[][] selections = {
                {"The Shining", "Stephen King"},
                {"It", "Stephen King"},
                {"Carrie", "Stephen King"},
                {"Misery", "Stephen King"},
                {"Pet Sematary", "Stephen King"},
                {"Salem's Lot", "Stephen King"},
                {"The Stand", "Stephen King"},
                {"The Dead Zone", "Stephen King"},
                {"Cujo", "Stephen King"},
                {"Christine", "Stephen King"},
                {"Doctor Sleep", "Stephen King"},
                {"11/22/63", "Stephen King"}
        };

        // load the stephen king collection using its own book matching method
        loadStephenKingBooks(
                recyclerView,
                selections,
                0);
    }

    private void setupAllTimeFavourites() {
        RecyclerView recyclerView =
                findViewById(R.id.favouritesRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        // retrieve classic fiction books using one google books search
        googleBooksService.getBooks(
                "subject:classic fiction",
                0,
                40,
                "relevance",
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> results) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // add books with covers and avoid duplicates
                        addCollectionBooks(results, favouriteBooks, 15);

                        recyclerView.setAdapter(
                                new BookAdapter(
                                        new ArrayList<>(favouriteBooks)));

                        android.util.Log.d("BOOKMARK_LOADING",
                                "favourites loaded: " + favouriteBooks.size());

                        markCollectionReady(favouriteBooks);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        android.util.Log.e("BOOKMARK_LOADING",
                                "favourites failed: " + message);

                        markCollectionReady(favouriteBooks);
                    }
                });
    }

    private void loadCuratedBooks(
            RecyclerView recyclerView,
            String[][] selections,
            int index,
            ArrayList<Book> selectedBooks) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (index >= selections.length) {
            if (selectedBooks.isEmpty()) {
                Toast.makeText(this,
                        "No matching books found.",
                        Toast.LENGTH_SHORT).show();
            }

            // mark the collection ready if all api searches have finished
            markCollectionReady(selectedBooks);

            return;
        }

        String title = selections[index][0];
        String author = selections[index][1];

        String query = "intitle:\"" + title
                + "\" inauthor:\"" + author + "\"";

        android.util.Log.d("BOOKMARK_LOADING",
                "searching for book: " + title);

        googleBooksService.getBooks(
                query,
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> results) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        boolean found = false;

                        // find a matching edition with a cover
                        for (Book book : results) {
                            if (matchesBook(book, title, author)) {
                                selectedBooks.add(book);
                                found = true;

                                recyclerView.setAdapter(
                                        new BookAdapter(
                                                new ArrayList<>(selectedBooks)));

                                android.util.Log.d("BOOKMARK_LOADING",
                                        "loaded book: " + title);

                                break;
                            }
                        }

                        if (!found) {
                            android.util.Log.w("BOOKMARK_LOADING",
                                    "no matching book found: " + title);
                        }

                        loadCuratedBooks(
                                recyclerView,
                                selections,
                                index + 1,
                                selectedBooks);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        android.util.Log.e("BOOKMARK_LOADING",
                                "failed to load: " + title
                                        + " by " + author
                                        + " | error: " + message);

                        // continue loading the remaining books
                        loadCuratedBooks(
                                recyclerView,
                                selections,
                                index + 1,
                                selectedBooks);
                    }
                });
    }

    // add books with covers and avoid repeated titles
    private void addCollectionBooks(
            ArrayList<Book> results,
            ArrayList<Book> collection,
            int limit) {

        collection.clear();

        for (Book book : results) {

            if (book == null
                    || book.getTitle() == null
                    || book.getTitle().trim().isEmpty()
                    || book.getCoverUrl() == null
                    || book.getCoverUrl().trim().isEmpty()) {
                continue;
            }

            boolean alreadyAdded = false;

            for (Book existing : collection) {
                if (normalise(existing.getTitle())
                        .equals(normalise(book.getTitle()))) {

                    alreadyAdded = true;
                    break;
                }
            }

            if (!alreadyAdded) {
                collection.add(book);
            }

            if (collection.size() >= limit) {
                break;
            }
        }
    }

    private boolean matchesBook(
            Book book,
            String expectedTitle,
            String expectedAuthor) {

        if (book == null) {
            return false;
        }

        String title = normalise(book.getTitle());
        String author = normalise(book.getAuthors());

        String wantedTitle = normalise(expectedTitle);
        String wantedAuthor = normalise(expectedAuthor);

        boolean titleMatches =
                title.equals(wantedTitle)
                        || title.startsWith(wantedTitle + " ");

        boolean authorMatches =
                author.replace(" ", "")
                        .contains(wantedAuthor.replace(" ", ""));

        boolean hasCover =
                book.getCoverUrl() != null
                        && !book.getCoverUrl().trim().isEmpty();

        return titleMatches && authorMatches && hasCover;
    }

    private String normalise(String value) {
        if (value == null) {
            return "";
        }

        String normalised = Normalizer.normalize(
                value, Normalizer.Form.NFD);

        return normalised
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }


    // initialise the ambient light sensor for the homepage
    private void setupAmbientLightSensor() {

        View root = findViewById(R.id.main);

        // save the original text colours
        saveOriginalTextColours(root);

        // listen for changes in the surrounding light
        ambientLightHelper = new AmbientLightHelper(
                this,
                lux -> {

                    // switch to dark mode in low lighting
                    if (lux < 30 && !darkMode) {
                        applyBookTheme(true);
                    }

                    // return to light mode in brighter conditions
                    else if (lux > 150 && darkMode) {
                        applyBookTheme(false);
                    }
                }
        );

        if (!ambientLightHelper.isSensorAvailable()) {
            Log.d(
                    "AmbientLight",
                    "Ambient light sensor not available"
            );
        }
    }

    // save the original text colours from the layout
    private void saveOriginalTextColours(View view) {

        if (view instanceof TextView) {

            TextView textView = (TextView) view;

            originalTextColours.put(
                    textView,
                    textView.getTextColors()
            );
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                saveOriginalTextColours(group.getChildAt(i));
            }
        }
    }

    // save the original background before changing its colour
    private void saveOriginalBackground(View view) {

        if (view != null
                && !originalBackgrounds.containsKey(view)) {

            Drawable background = view.getBackground();

            originalBackgrounds.put(
                    view,
                    background != null
                            ? background.getConstantState() != null
                            ? background.getConstantState().newDrawable().mutate()
                            : background
                            : null
            );
        }
    }

    // change a view's background colour
    private void changeBackground(View view, int colour) {

        if (view == null) {
            return;
        }

        saveOriginalBackground(view);
        view.setBackgroundColor(colour);
    }

    // apply the light or dark theme to the homepage
    private void applyBookTheme(boolean dark) {

        darkMode = dark;

        LinearLayout mainLayout = findViewById(R.id.main);

        if (mainLayout == null) {
            return;
        }

        if (dark) {

            // change the main homepage background
            changeBackground(mainLayout, DARK_BACKGROUND);

            // find the scrolling book discovery content
            for (int i = 0; i < mainLayout.getChildCount(); i++) {

                View child = mainLayout.getChildAt(i);

                if (child instanceof ScrollView) {

                    ScrollView scrollView = (ScrollView) child;

                    changeBackground(
                            scrollView,
                            DARK_BACKGROUND
                    );

                    if (scrollView.getChildCount() > 0) {

                        changeBackground(
                                scrollView.getChildAt(0),
                                DARK_BACKGROUND
                        );
                    }
                }
            }

            // change section headings and see all labels
            updateHomeTextColours(mainLayout);

            // apply the dark theme to the latest reviews
            if (latestReviewsFeed != null) {
                latestReviewsFeed.setDarkMode(true);
            }

            Log.d(
                    "AmbientLight",
                    "Homepage dark theme activated"
            );

        } else {

            // restore the original homepage background
            mainLayout.setBackgroundColor(Color.WHITE);

            // restore the scrolling content background
            for (int i = 0; i < mainLayout.getChildCount(); i++) {

                View child = mainLayout.getChildAt(i);

                if (child instanceof ScrollView) {

                    ScrollView scrollView = (ScrollView) child;

                    scrollView.setBackgroundColor(Color.WHITE);

                    if (scrollView.getChildCount() > 0) {

                        scrollView.getChildAt(0)
                                .setBackgroundColor(Color.WHITE);
                    }
                }
            }

            // restore the original text colours
            for (Map.Entry<TextView, ColorStateList> entry :
                    originalTextColours.entrySet()) {

                entry.getKey().setTextColor(entry.getValue());
            }

            // restore the light theme of the latest reviews
            if (latestReviewsFeed != null) {
                latestReviewsFeed.setDarkMode(false);
            }

            Log.d(
                    "AmbientLight",
                    "Homepage light theme activated"
            );
        }
    }


    // update the text colours in the book discovery sections
    private void updateHomeTextColours(View view) {

        if (view instanceof TextView) {

            TextView textView = (TextView) view;

            // keep the white bookmark title unchanged
            if ("bookmark".contentEquals(textView.getText())) {
                return;
            }

            // keep the search bar's existing colours
            if (textView.getId() == R.id.searchBar) {
                return;
            }

            String text = textView.getText().toString();

            if (text.contains("see all")) {

                textView.setTextColor(
                        DARK_SECONDARY_TEXT
                );

            } else {

                textView.setTextColor(
                        DARK_TEXT
                );
            }
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {

                View child = group.getChildAt(i);

                // do not change the shared bottom navigation
                if (child instanceof LinearLayout
                        && child.getBackground() != null
                        && child != findViewById(R.id.main)) {

                    // continue processing its text
                }

                updateHomeTextColours(child);
            }
        }
    }

    // update the latest reviews placeholder background
    private void updateReviewsPanel(View view, boolean dark) {

        if (view instanceof FrameLayout) {

            FrameLayout panel = (FrameLayout) view;

            if (panel.getChildCount() == 1
                    && panel.getChildAt(0) instanceof TextView) {

                TextView textView =
                        (TextView) panel.getChildAt(0);

                if (textView.getText().toString()
                        .contains("latest reviews will appear here")) {

                    changeBackground(
                            panel,
                            dark ? DARK_PANEL : Color.rgb(244, 240, 247)
                    );
                }
            }
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {

                updateReviewsPanel(
                        group.getChildAt(i),
                        dark
                );
            }
        }
    }

    // start the sensor when the homepage becomes active
    @Override
    protected void onResume() {
        super.onResume();

        if (ambientLightHelper != null) {
            ambientLightHelper.start();
        }

        // retrieve the three most recent reviews
        if (latestReviewsFeed != null) {
            latestReviewsFeed.loadReviews(3);
        }
    }

    // stop listening when the homepage is no longer active
    @Override
    protected void onPause() {

        if (ambientLightHelper != null) {
            ambientLightHelper.stop();
        }

        if (latestReviewsFeed != null) {
            latestReviewsFeed.stopListening();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (nytBooksService != null) {
            nytBooksService.shutdown();
        }

        if (googleBooksService != null) {
            googleBooksService.shutdown();
        }

        if (latestReviewsFeed != null) {
            latestReviewsFeed.shutdown();
        }
    }
}