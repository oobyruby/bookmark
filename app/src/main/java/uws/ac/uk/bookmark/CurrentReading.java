
package uws.ac.uk.bookmark;

//firebase firestore imports
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class CurrentReading extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current);
        // Connect the bottom navigation buttons
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
            Intent intent = new Intent(CurrentReading.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // open explore
        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(CurrentReading.this, ExploreActivity.class);
            startActivity(intent);
        });

        // opens map screen
        navMap.setOnClickListener(v -> {
            startActivity(new Intent(CurrentReading.this, NearbyLibrariesActivity.class));
        });
        // Open Profile screen
        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(CurrentReading.this, ProfileActivity.class));
        });

        //Connect to the Recycler View in activity_current.xml
        RecyclerView recyclerView = findViewById(R.id.currentBooksRecyclerView);

        //set the list to display book vertically
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //create an empty list to hold bbooks loaded from firebase
        List<CurrentBook> bookList = new ArrayList<>();

        //connects the adapter to the recycler
        CurrentBooksAdapter adapter = new CurrentBooksAdapter(bookList);
        recyclerView.setAdapter(adapter);

        //connect to firebase firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please log in first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //get the current loging user Firebase ID

        // Find the user's currently reading list
        db.collection("readingLists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("listType", "currently_reading")
                .limit(1)
                .get()
                .addOnSuccessListener(readingLists -> {

                    if (readingLists.isEmpty()) {
                        return;
                    }

                    String listId = readingLists.getDocuments().get(0).getId();

                    // find reading entries beloning to users
                    db.collection("readingEntries")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("listId", listId)
                            .whereEqualTo("completed", false)
                            .get()

                            //Runs this when firebase successfully loads the entries
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                //go through each reading story
                                for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                                    // Get the reading progress from Firestore
                                    Long progress = document.getLong("progressPercent");
                                    // use 0 if no progress has been saved
                                    int processPercent = progress != null ? progress.intValue() : 0;

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
                                                            "CurrentReading",
                                                            "Could not load book details",
                                                            task.getException());
                                                }

                                                //Create a book for this reading entry
                                                CurrentBook book = new CurrentBook(
                                                        document.getId(),
                                                        title,
                                                        authors,
                                                        coverUrl,
                                                        processPercent
                                                );
                                                //add this book to the list
                                                bookList.add(book);
                                                adapter.notifyDataSetChanged();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "Could not load reading entries: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load reading list: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

    }
}
