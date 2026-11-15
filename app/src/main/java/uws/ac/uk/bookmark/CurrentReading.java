package uws.ac.uk.bookmark;

//firebase firestore imports
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //get the current loging user Firebase ID

        // find reading entries beloning to users
        db.collection("readingEntries")
                .whereEqualTo("userId", userId)
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

                        //Create a book for this reading entry
                        CurrentBook book = new CurrentBook(
                                document.getId(),
                                "Test Book",
                                "Book details coming soon",
                                "",
                                processPercent
                        );
                        //add this book to the list
                        bookList.add(book);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Could not load reading entries: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

    }
}