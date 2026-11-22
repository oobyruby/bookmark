package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import android.graphics.Color;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SeeAllActivity extends AppCompatActivity {

    //RecylerView  display listed of completed books
    private RecyclerView completedBooksRecyclerView;

    //TextView display a message when there are no completed books
    private TextView emptyCompletedBooks;

    //ArrayLists stores completed books retrieved from firestore
    private ArrayList<CurrentBook> completedBooks;

    //adapter that display completed books
    private CompletedBooksAdapter completedBooksAdapter;

    //connect to firebase
    private FirebaseFirestore db;

    //get the currently signed-in user
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //connect this activity to See All XML layout
        setContentView(R.layout.activity_see_all);
        // Match the Android system bars to the purple theme
        getWindow().setStatusBarColor(Color.parseColor("#BCA9CC"));
        getWindow().setNavigationBarColor(Color.parseColor("#BCA9CC"));
        getWindow().setNavigationBarContrastEnforced(false);

// Use dark status bar icons
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

// Keep the layout clear of the system bars
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
        // Connect the bottom navigation buttons
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);
        // Connect the back button
        TextView backButton = findViewById(R.id.backButton);


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Return to Profile screen
        backButton.setOnClickListener(v -> {
            finish();
        });
        // return to homepage
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(SeeAllActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // open explore
        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(SeeAllActivity.this, ExploreActivity.class);
            startActivity(intent);
        });

        // opens map screen
        navMap.setOnClickListener(v -> {
            startActivity(new Intent(SeeAllActivity.this, NearbyLibrariesActivity.class));
        });
        // Open Profile screen
        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(SeeAllActivity.this, ProfileActivity.class));
        });

        //find the empty state message from the XML layout
        emptyCompletedBooks = findViewById(R.id.emptyCompletedBooks);

        //find the RecyclerView from the XML layout
        completedBooksRecyclerView = findViewById(R.id.completedBooksRecyclerView);


        //create an empty list to hold completed book
        completedBooks = new ArrayList<>();



        //Arrange the books vertically in the RecyclerView
        completedBooksRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        //connect the completed books list to the adapter
        completedBooksAdapter = new CompletedBooksAdapter(completedBooks);
        //display the adapter
        completedBooksRecyclerView.setAdapter(completedBooksAdapter);
        //Load completed books from firestore
        loadCompletedBooks();

    }
    //load all completed books belonging to the user signed in
    private void loadCompletedBooks(){

        //check that a users is signed in
        if (auth.getCurrentUser() == null){
            return;
        }
        // get the current users ID
        String userId = auth.getCurrentUser().getUid();

        // Find completed reading entries for this user
        db.collection("readingEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(querySnapshot ->{

                    //check whether any completed books were found
                    if(querySnapshot.isEmpty()){
                        showCompletedBooks();
                        return;
                    }

                    //Go through each completed reading entry
                    for (QueryDocumentSnapshot document : querySnapshot){
                        //get the linked book reference id
                        String bookRefId = document.getString("bookRefId");


                        // check that the book reference ID exist
                        if (bookRefId != null){
                            // find the matching book document
                            db.collection("bookReferences")
                                    .document(bookRefId)
                                    .get()
                                    .addOnSuccessListener(bookDocument->{
                                        //check that the book document exist
                                        if (bookDocument.exists()){

                                            // get the book title, author and book cover url
                                            String title = bookDocument.getString("title");
                                            String author = bookDocument.getString("author");
                                            String coverUrl = bookDocument.getString("coverUrl");
                                            String externalBookId = bookDocument.getString("externalBookId");

                                            //add the completed book to list
                                            CurrentBook book = new CurrentBook(
                                                    document.getId(),
                                                    title,
                                                    author,
                                                    coverUrl,
                                                    100,
                                                    externalBookId
                                            );
                                            completedBooks.add(book);
                                            //tell the adapter to refresh list
                                            completedBooksAdapter.notifyDataSetChanged();
                                            showCompletedBooks();

                                        }

                                    });

                        }


                    }
                });

    }
    private void showCompletedBooks(){
        //shoe a message if there are no completed books
        if(completedBooks.isEmpty()){
            emptyCompletedBooks.setVisibility(View.VISIBLE);
            completedBooksRecyclerView.setVisibility(View.GONE);
        } else{
            emptyCompletedBooks.setVisibility(View.GONE);
            completedBooksRecyclerView.setVisibility(View.VISIBLE);
        }
    }

}
