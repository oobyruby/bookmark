package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;

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
            Intent intent = new Intent(ProfileActivity.this, LatestReads.class );
            startActivity(intent);
        });

        //Initialise Firebase Authenticate
        auth = FirebaseAuth.getInstance();

        //Initialise Cloud Firestore
        db = FirebaseFirestore.getInstance();

        //Connect to the firebase storage bucket
        storage = FirebaseStorage.getInstance(
                "gs://bookmark-387d8.firebasestorage.app"
        );
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

        // map will be connected when its screen exists
        navMap.setOnClickListener(v -> {
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
                        //display the picture using glide
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
}
