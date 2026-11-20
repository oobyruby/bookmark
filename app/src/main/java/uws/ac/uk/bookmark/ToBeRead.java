package uws.ac.uk.bookmark;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import android.content.Intent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

public class ToBeRead extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toberead);
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);
        // Connect the back button
        TextView backButton = findViewById(R.id.backButton);

        // Return to Profile screen
        backButton.setOnClickListener(v -> {
            finish();
        });
        // return to homepage
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ToBeRead.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // open explore
        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(ToBeRead.this, ExploreActivity.class);
            startActivity(intent);
        });

        // opens map screen
        navMap.setOnClickListener(v -> {
            startActivity(new Intent(ToBeRead.this, NearbyLibrariesActivity.class));
        });
        // Open Profile screen
        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(ToBeRead.this, ProfileActivity.class));
        });


        // Connect to the RecyclerView in toberead.xml
        RecyclerView recyclerView = findViewById(R.id.toBeReadRecyclerView);

        // Display books vertically
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //create an empty list to hold the to be read books
        List<CurrentBook> bookList = new ArrayList<>();

        //connect the book list to the existing adapter
        ToBeReadAdapter adapter = new ToBeReadAdapter(bookList);      //display the adapter
        recyclerView.setAdapter(adapter);


        //connect to firebase firestore
        FirebaseFirestore db =FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please log in first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Get te id of the currently logged-in user
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Find the To Be Read list belonging to this user
        db.collection("readingLists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("listType", "to_be_read")
                .limit(1)
                .get()

                // Run when Firestore finds the list
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Check whether the list exists
                    if (!queryDocumentSnapshots.isEmpty()) {

                        // Get the ID of the To Be Read list
                        String listId = queryDocumentSnapshots
                                .getDocuments().get(0).getId();

                        // Find books belonging to this list
                        db.collection("readingEntries")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("listId", listId)
                                .whereEqualTo("completed", false)
                                .get()

                                // Run when the books are loaded
                                .addOnSuccessListener(bookSnapshots -> {

                                    // Go through each book entry
                                    for (QueryDocumentSnapshot document : bookSnapshots) {

                                        // Get the saved reading progress
                                        Long progress = document.getLong("progressPercent");

                                        // Use 0 if progress is missing
                                        int progressPercent = progress != null
                                                ? progress.intValue() : 0;

                                        // Get the book reference ID
                                        String bookRefId = document.getString("bookRefId");

                                        if (bookRefId == null || bookRefId.trim().isEmpty()
                                                || bookRefId.contains("/")) {
                                            continue;
                                        }

                                        // Retrieve the saved book information
                                        db.collection("bookReferences")
                                                .document(bookRefId)
                                                .get()
                                                .addOnCompleteListener(task -> {

                                                    if (isFinishing() || isDestroyed()) {
                                                        return;
                                                    }

                                                    String title = "Book details unavailable";
                                                    String authors = "Author details unavailable";
                                                    String coverUrl = "";

                                                    if (task.isSuccessful()) {

                                                        DocumentSnapshot bookReference =
                                                                task.getResult();

                                                        if (bookReference != null && bookReference.exists()) {

                                                            String savedTitle =
                                                                    bookReference.getString("title");

                                                            String savedAuthors =
                                                                    bookReference.getString("authors");

                                                            String savedCover =
                                                                    bookReference.getString("coverUrl");

                                                            if (savedTitle != null && !savedTitle.isEmpty()) {
                                                                title = savedTitle;
                                                            }

                                                            if (savedAuthors != null && !savedAuthors.isEmpty()) {
                                                                authors = savedAuthors;
                                                            }

                                                            if (savedCover != null) {
                                                                coverUrl = savedCover;
                                                            }
                                                        }
                                                    } else {
                                                        android.util.Log.e(
                                                                "ToBeRead",
                                                                "Could not load book details",
                                                                task.getException());
                                                    }

                                                    // Create a book with the saved details
                                                    CurrentBook book = new CurrentBook(
                                                            document.getId(),
                                                            title,
                                                            authors,
                                                            coverUrl,
                                                            progressPercent
                                                    );

                                                    // Add the book to the list
                                                    bookList.add(book);

                                                    // Refresh the RecyclerView
                                                    adapter.notifyDataSetChanged();
                                                });
                                    }
                                })

                                // Show an error if books cannot be loaded
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Could not load books: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });

                    } else {

                        // No To Be Read list was found
                        Toast.makeText(this,
                                "No To Be Read list found.",
                                Toast.LENGTH_SHORT).show();
                    }
                })

                // Show an error if the list cannot be loaded
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load reading list: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

    }}