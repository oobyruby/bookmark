package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private GoogleBooksService googleBooksService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigation();
        setupTrendingBooks();
    }

    private void setupNavigation() {
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            // the homepage is already open
        });

        navExplore.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExploreActivity.class)));

        navMap.setOnClickListener(v -> {
            // connect when the map screen exists
        });

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }

    private void setupTrendingBooks() {
        RecyclerView recyclerView = findViewById(R.id.trendingBooksRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        googleBooksService = new GoogleBooksService();

        // temporary API test; this is not a dedicated trending-books feed
        googleBooksService.getBooks("bestselling fiction", new GoogleBooksService.BooksCallback() {
            @Override
            public void onSuccess(ArrayList<Book> books) {
                recyclerView.setAdapter(new BookAdapter(books));

                if (books.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "No books found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this,
                        "Could not load books: " + message,
                        Toast.LENGTH_LONG).show();
            }
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
