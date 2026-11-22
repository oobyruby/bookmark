package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;


import android.graphics.Color;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.ContentValues;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    //used to display thr user's profile picture
    private ImageView profileImage;

    // used to display the user's  username and handle
    private TextView profileUsername;
    private TextView profileHandle;

    // Firebase services
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    // Displays the completed books under Latest Reads
    private RecyclerView latestReadsRecyclerView;

    // Stores the completed books loaded from Firestore
    private ArrayList<CurrentBook> latestReadsList;

    // Adapter displays each book cover
    private ProfileCompletedBooksAdapter profileCompletedBooksAdapter;
    //used to open the device's image picker
    private ActivityResultLauncher<String[]> imagePicker;

    //opens the device camera to take photo
    private ActivityResultLauncher<Uri> cameraLauncher;
    //stores the location of new image
    private Uri cameraImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // Connect the Completed Books RecyclerView to the XML layout
        latestReadsRecyclerView = findViewById(R.id.completedBooksRecyclerView);

// Display the book covers horizontally
        latestReadsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

// Allow the horizontal RecyclerView to work inside the ScrollView
        latestReadsRecyclerView.setNestedScrollingEnabled(false);

// Create a list to store completed books
        latestReadsList = new ArrayList<>();

// Create the Profile adapter using the completed books list
        profileCompletedBooksAdapter = new ProfileCompletedBooksAdapter(latestReadsList);

// Connect the adapter to the RecyclerView
        latestReadsRecyclerView.setAdapter(profileCompletedBooksAdapter);

        // match the android system bars to the bookmark theme
        getWindow().setStatusBarColor(Color.parseColor("#BCA9CC"));
        getWindow().setNavigationBarColor(Color.parseColor("#BCA9CC"));
        getWindow().setNavigationBarContrastEnforced(false);

        // use dark status bar icons
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        // keep the profile layout clear of the system bars
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



        View seeAllLatestReads =findViewById(R.id.seeAllLatestReads);
        View currentlyReadingRow = findViewById(R.id.currentlyReadingRow);
        View toBeReadRow =findViewById(R.id.toBeReadRow);
        //open currently reading page
        currentlyReadingRow.setOnClickListener(v ->{
            Intent intent =new Intent(ProfileActivity.this, CurrentReading.class);
            startActivity(intent);
        });
        // Open to be Read page
        toBeReadRow.setOnClickListener( v-> {
            Intent intent = new Intent(ProfileActivity.this, ToBeRead.class);
            startActivity(intent);
        });
        seeAllLatestReads.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, SeeAllActivity.class );
            startActivity(intent);
        });

        //Initialise Firebase Authenticate
        auth = FirebaseAuth.getInstance();

        //Initialise Cloud Firestore
        db = FirebaseFirestore.getInstance();
        // Load the user's completed books for the Profile page
        loadCompletedBooks();

        //Connect to the firebase storage bucket
        storage = FirebaseStorage.getInstance(
                "gs://bookmark-387d8.firebasestorage.app"
        );


        // connect the logout button
        View logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {

            // confirm before signing the user out
            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("log out")
                    .setMessage("are you sure you want to log out?")
                    .setNegativeButton("cancel", null)
                    .setPositiveButton("log out", (dialog, which) -> {

                        // sign out of firebase
                        auth.signOut();

                        // clear the google credential selection
                        GoogleAuthHelper.clearCredentialState(
                                ProfileActivity.this);


                        // return to the login screen
                        Intent intent = new Intent(
                                ProfileActivity.this,
                                LoginActivity.class
                        );

                        // clear previous screens
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    })
                    .show();
        });

        //connect the profile picture to the XML layout
        profileImage = findViewById(R.id.profileImage);
        //load the user's profile pictures
        loadProfilePicture();

        profileUsername = findViewById(R.id.profileUsername);
        profileHandle = findViewById(R.id.profileHandle);
        loadProfileDetails();

        //connect the change profile picture button
        ImageButton changePicture = findViewById(R.id.changeProfilePicture);
        // show the users their image
        changePicture.setOnClickListener(v-> showImageOptions());

        //Register the gallery image picker
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                // code runs after the user selects an image
                uri ->{
                    if (uri != null){
                        //send the selected image to our upload method
                        uploadProfilePicture(uri);
                    }
                }
        );
        //register the camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),

                // code runs after the camera takes photo
                success ->{
                    if (success && cameraImageUri != null){
                        uploadProfilePicture(cameraImageUri);
                    }
                }
        );


        // bottom navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // return to homepage
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // open explore
        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ExploreActivity.class);
            startActivity(intent);
        });

        // opeens map screen
        navMap.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, NearbyLibrariesActivity.class));
        });


        // profile is already open
        navProfile.setOnClickListener(v -> {
        });
    }
    // create a location where the camera saves the new image
    private Uri createCameraImageUri(){
        //prepare details about the image
        ContentValues values =new ContentValues();

        //Give the image a unique filename
        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "profile_" + System.currentTimeMillis() + ".jpg"
        );
        //specify that the image is a JPEG
        values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
        );
        //create the image location and return its Uri
        return getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

    }
    //Displays the gallery and camera options
    private void showImageOptions(){

        // choices displayed to user
        String[] options ={
                "Choose from gallery",
                "Take a photo"
        };
        //Display the option in a dialog
        new AlertDialog.Builder(this)
                .setTitle("Change profile picture")
                .setItems(options,(dialog, which)->{
            if (which ==0){
                //open the gallery
                imagePicker.launch(new String[]{"image/*"});
            } else if (which == 1) {
                // connect to the camera
                cameraImageUri = createCameraImageUri();
                //check that image location was created
                if(cameraImageUri != null){
                    //open camera
                    cameraLauncher.launch(cameraImageUri);
                }else{
                    Toast.makeText(
                            ProfileActivity.this,
                            "Could not create image location",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        })
                .show();

    }
    private void uploadProfilePicture(Uri imageUri){
        //get the signin user's ID
        String userId = auth.getCurrentUser().getUid();

        //create a reference to the user profile image in firebase storage
        StorageReference imageRef = storage.getReference()
                .child("profilePictures")
                .child(userId)
                .child("profile.jpg");
        imageRef.putFile(imageUri)
                //check whether the upload successful
                .addOnSuccessListener(taskSnapshot -> {
                    //image uploaded successfully
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {

                        // Get the image URL
                        String photoUrl = uri.toString();

                        // Save the URL in Firestore
                        db.collection("users")
                                .document(userId)
                                .update("photoUrl", photoUrl)
                                .addOnSuccessListener(aVoid ->{

                                    //Display the uploaded profile picture
                                    Glide.with(ProfileActivity.this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .into(profileImage);
                                });

                    }); // Now close the section here
                })
                // handle any error during upload
                .addOnFailureListener(e->{
                    Toast.makeText(
                            ProfileActivity.this,
                            "Image upload failed:" +e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    // load the saved profile picture
    private void loadProfilePicture(){
        //Get the signed-in  user's ID
        String userId = auth.getCurrentUser().getUid();

        //find the user's document in Firestore
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document->{
                    //get the saved image URL
                    String photoUrl = document.getString("photoUrl");

                    //check whether a picture exist
                    if (photoUrl != null){
                        //remove background when profile image exist
                        findViewById(R.id.profileImageContainer)
                                .setBackgroundColor(Color.TRANSPARENT);
                        //display the picture
                        Glide.with(ProfileActivity.this)
                                .load(photoUrl)
                                .circleCrop()
                                .into(profileImage);


                    }

                });
    }
    private void loadProfileDetails(){
        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document ->{
                    if (document.exists()){
                        String username = document.getString("username");

                        if (username != null){
                            profileUsername.setText(username);
                            profileHandle.setText("@"+username);
                        }
                    }

                });
    }

    // Load all completed books belonging to the current user
    private void loadCompletedBooks() {

        // Check that a user is signed in
        if (auth.getCurrentUser() == null) {
            return;
        }

        // Get the current user's Firebase ID
        String userId = auth.getCurrentUser().getUid();

        // Find this user's completed reading entries in Firestore
        db.collection("readingEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    // Clear the current list before loading books
                    latestReadsList.clear();

                    // If there are no completed books, refresh the RecyclerView
                    if (querySnapshot.isEmpty()) {
                        profileCompletedBooksAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Go through each completed reading entry
                    for (com.google.firebase.firestore.DocumentSnapshot entry
                            : querySnapshot.getDocuments()) {

                        // Get the ID of the linked book reference
                        String bookRefId = entry.getString("bookRefId");

                        // Skip entries without a linked book reference
                        if (bookRefId == null || bookRefId.isEmpty()) {
                            continue;
                        }

                        // Load the book details from the bookReferences collection
                        db.collection("bookReferences")
                                .document(bookRefId)
                                .get()
                                .addOnSuccessListener(bookDoc -> {

                                    // Check that the book document exists
                                    if (!bookDoc.exists()) {
                                        return;
                                    }

                                    // Read the book details from Firestore
                                    String title = bookDoc.getString("title");
                                    String author = bookDoc.getString("author");
                                    String coverUrl = bookDoc.getString("coverUrl");
                                    String externalBookId = bookDoc.getString("externalBookId");

                                    // Create a CurrentBook using the reading entry's document ID
                                    CurrentBook book = new CurrentBook(
                                            entry.getId(),
                                            title,
                                            author,
                                            coverUrl,
                                            100,
                                            externalBookId
                                    );

                                    // Add the completed book to the list
                                    latestReadsList.add(book);

                                    // Refresh the RecyclerView to display its cover
                                    profileCompletedBooksAdapter.notifyDataSetChanged();
                                });
                    }
                })
                .addOnFailureListener(e -> {

                    // Show a message if completed books cannot be loaded
                    Toast.makeText(
                            ProfileActivity.this,
                            "Could not load completed books",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

}
