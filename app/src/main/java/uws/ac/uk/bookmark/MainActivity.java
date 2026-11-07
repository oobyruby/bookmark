package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

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

    private final ArrayList<Book> trendingBooks = new ArrayList<>();
    private final ArrayList<Book> newReleaseBooks = new ArrayList<>();
    private final ArrayList<Book> favouriteBooks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleBooksService = new GoogleBooksService();

        setupNavigation();
        setupHomeSearch();
        setupSeeAllButtons();
        setupTrendingBooks();
        setupNewReleases();
        setupAllTimeFavourites();
    }

    // Handle book searches from the Home page
    private void setupHomeSearch() {

        EditText searchBar = findViewById(R.id.searchBar);

        // Handle the Search button on the keyboard
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

                    // Open Search Results and pass the search query
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

        findViewById(R.id.seeAllFavourites).setOnClickListener(v ->
                openBookList("all-time favourites", favouriteBooks));
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

                        Toast.makeText(MainActivity.this,
                                "Could not load trending books: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupNewReleases() {
        RecyclerView recyclerView =
                findViewById(R.id.newReleasesRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        // curated mainstream fiction
        String[][] selections = {
                {"Great Big Beautiful Life", "Emily Henry"},
                {"Onyx Storm", "Rebecca Yarros"},
                {"Sunrise on the Reaping", "Suzanne Collins"},
                {"The Secret of Secrets", "Dan Brown"},
                {"Atmosphere", "Taylor Jenkins Reid"},
                {"The Names", "Florence Knapp"}
        };

        loadCuratedBooks(
                recyclerView,
                selections,
                0,
                newReleaseBooks);
    }

    private void setupAllTimeFavourites() {
        RecyclerView recyclerView =
                findViewById(R.id.favouritesRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this, LinearLayoutManager.HORIZONTAL, false));

        // curated all-time favourite books
        String[][] selections = {
                {"Harry Potter and the Philosopher's Stone", "J. K. Rowling"},
                {"Pride and Prejudice", "Jane Austen"},
                {"The Hobbit", "J. R. R. Tolkien"},
                {"The Hunger Games", "Suzanne Collins"},
                {"Little Women", "Louisa May Alcott"},
                {"The Seven Husbands of Evelyn Hugo", "Taylor Jenkins Reid"},
                {"The Great Gatsby", "F. Scott Fitzgerald"},
                {"The Lord of the Rings", "J. R. R. Tolkien"},
                {"To Kill a Mockingbird", "Harper Lee"},
                {"The Book Thief", "Markus Zusak"},
                {"1984", "George Orwell"},
                {"Twilight", "Stephenie Meyer"},
                {"Jane Eyre", "Charlotte Bronte"},
                {"The Fault in Our Stars", "John Green"},
                {"The Picture of Dorian Gray", "Oscar Wilde"}
        };

        loadCuratedBooks(
                recyclerView,
                selections,
                0,
                favouriteBooks);
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
            return;
        }

        String title = selections[index][0];
        String author = selections[index][1];

        String query = "intitle:\"" + title
                + "\" inauthor:\"" + author + "\"";

        googleBooksService.getBooks(
                query,
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> results) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        // find a matching edition with a cover
                        for (Book book : results) {
                            if (matchesBook(book, title, author)) {
                                selectedBooks.add(book);

                                recyclerView.setAdapter(
                                        new BookAdapter(
                                                new ArrayList<>(selectedBooks)));

                                break;
                            }
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

                        // continue loading the remaining books
                        loadCuratedBooks(
                                recyclerView,
                                selections,
                                index + 1,
                                selectedBooks);
                    }
                });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (nytBooksService != null) {
            nytBooksService.shutdown();
        }

        if (googleBooksService != null) {
            googleBooksService.shutdown();
        }
    }
}

