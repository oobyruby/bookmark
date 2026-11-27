
package uws.ac.uk.bookmark;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

// audio player imports
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Locale;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class BookViewActivity extends AppCompatActivity {

    // Firebase listeners for app ratings and reviews
    private ListenerRegistration bookReferenceListener;
    private ListenerRegistration reviewsListener;

    private boolean hasAppRating = false;
    private String googleBooksRating = "not yet rated";

    // monitor ambient lighting on the book details screen
    private AmbientLightHelper ambientLightHelper;
    private boolean darkThemeEnabled = false;

    // locally stored narrated synopsis player
    private MediaPlayer audioPlayer;
    private SeekBar audioSeekBar;
    private TextView audioPlayPauseButton;
    private TextView audioCurrentTime;
    private TextView audioTotalTime;

    // update the playback progress while the audio is playing
    private final Handler audioHandler = new Handler(Looper.getMainLooper());

    private final Runnable audioProgressUpdater = new Runnable() {
        @Override
        public void run() {
            if (audioPlayer != null) {
                try {
                    int position = audioPlayer.getCurrentPosition();

                    if (audioSeekBar != null) {
                        audioSeekBar.setProgress(position);
                    }

                    if (audioCurrentTime != null) {
                        audioCurrentTime.setText(formatAudioTime(position));
                    }

                    if (audioPlayer.isPlaying()) {
                        audioHandler.postDelayed(this, 250);
                    }
                } catch (IllegalStateException e) {
                    android.util.Log.e(
                            "BookViewActivity",
                            "Could not update audio progress",
                            e
                    );
                }
            }
        }
    };

    // Information passed from the Search Results screen
    public static final String EXTRA_TITLE = "book_title";
    public static final String EXTRA_AUTHORS = "book_authors";
    public static final String EXTRA_COVER_URL = "book_cover_url";
    public static final String EXTRA_DESCRIPTION = "book_description";
    public static final String EXTRA_ID = "book_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_view);

        // initialise the ambient light sensor
        ambientLightHelper = new AmbientLightHelper(this, lux -> {
            Log.d("AmbientLight", "Light level: " + lux + " lux");

            if (lux < 30 && !darkThemeEnabled) {
                applyBookTheme(true);
            } else if (lux > 150 && darkThemeEnabled) {
                applyBookTheme(false);
            }
        });

        if (!ambientLightHelper.isSensorAvailable()) {
            Log.d("AmbientLight", "Light sensor not available on this device");
        }

        // Connect Java to the XML layout
        ImageView bookCover = findViewById(R.id.bookCover);
        TextView bookTitle = findViewById(R.id.bookTitle);
        TextView bookAuthors = findViewById(R.id.bookAuthors);
        TextView bookDescription = findViewById(R.id.bookDescription);
        TextView readMoreButton = findViewById(R.id.readMoreButton);
        TextView bookRating = findViewById(R.id.bookRating);
        TextView backButton = findViewById(R.id.backButton);

        // Connect the reading list and review elements
        TextView addToShelfButton = findViewById(R.id.addToShelfButton);
        TextView currentlyReadingButton = findViewById(R.id.currentlyReadingButton);
        RatingBar bookReviewRating = findViewById(R.id.bookReviewRating);
        TextView writeReviewButton = findViewById(R.id.writeReviewButton);

        // Get the selected book's information
        Intent intent = getIntent();

        String title = intent.getStringExtra(EXTRA_TITLE);
        String authors = intent.getStringExtra(EXTRA_AUTHORS);
        String coverUrl = intent.getStringExtra(EXTRA_COVER_URL);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);

        // check whether the selected book has a custom stephen king synopsis
        String customSynopsis = StephenKingSynopses.getSynopsis(title, authors);

// override the api description for the five selected books
        if (customSynopsis != null) {
            description = customSynopsis;
        }

        // Retrieve the selected book's ID
        String bookId = intent.getStringExtra(EXTRA_ID);

        Book selectedBook = new Book(
                bookId,
                title,
                authors,
                description,
                coverUrl
        );

        // Display the book title
        if (title != null && !title.isEmpty()) {
            bookTitle.setText(title.toLowerCase(Locale.ROOT));
        } else {
            bookTitle.setText(R.string.book_title_not_available);
        }

        // Display the author
        if (authors != null && !authors.isEmpty()) {
            bookAuthors.setText(authors.toLowerCase(Locale.ROOT));
        } else {
            bookAuthors.setText(R.string.author_not_available);
        }

        // Display the book cover using Glide
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(bookCover);
        } else {
            bookCover.setImageResource(
                    R.drawable.genre_tile_background
            );
        }


        // Display the book description
        if (description != null && !description.isEmpty()) {
            bookDescription.setText(
                    Html.fromHtml(
                            description,
                            Html.FROM_HTML_MODE_COMPACT
                    )
            );
        } else {
            bookDescription.setText(
                    R.string.book_description_not_available
            );
        }

        // expand or collapse longer descriptions
        bookDescription.setMaxLines(6);
        bookDescription.setEllipsize(TextUtils.TruncateAt.END);

        bookDescription.post(() -> {
            if (bookDescription.getLayout() == null) {
                return;
            }

            int lastVisibleLine = bookDescription.getLineCount() - 1;

            boolean isTruncated = lastVisibleLine >= 0
                    && bookDescription.getLayout()
                    .getEllipsisCount(lastVisibleLine) > 0;

            readMoreButton.setVisibility(
                    isTruncated ? View.VISIBLE : View.GONE
            );
        });

        readMoreButton.setOnClickListener(v -> {
            boolean isExpanded =
                    "read less ‹".contentEquals(readMoreButton.getText());

            if (isExpanded) {
                bookDescription.setMaxLines(6);
                bookDescription.setEllipsize(TextUtils.TruncateAt.END);
                readMoreButton.setText("read more ›");
            } else {
                bookDescription.setMaxLines(Integer.MAX_VALUE);
                bookDescription.setEllipsize(null);
                readMoreButton.setText("read less ‹");
            }
        });

        // return to the previous screen
        backButton.setOnClickListener(v -> {
            finish();
        });


        // Add the selected book to the to be read list
        addToShelfButton.setOnClickListener(v ->
                addBookToShelf(
                        selectedBook,
                        "to be read"
                ));

        // Open the currently reading page
        currentlyReadingButton.setOnClickListener(v ->
                addBookToShelf(
                        selectedBook,
                        "currently reading"
                ));

        // Open the review dialog when a star rating is selected
        bookReviewRating.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    if (fromUser && rating > 0) {
                        SearchResultsAdapter.showReviewDialog(
                                ratingBar,
                                selectedBook,
                                rating
                        );

                        ratingBar.setRating(0);
                    }
                });

        // Open the review dialog when the write review button is selected
        writeReviewButton.setOnClickListener(v ->
                SearchResultsAdapter.showReviewDialog(
                        v,
                        selectedBook,
                        bookReviewRating.getRating()
                ));

        // retrieve the book rating from google books
        loadBookRating(title, authors, bookRating);

        // set up the narrated synopsis player for the five selected books
        setupAudioPlayer(title, authors);

        // Bottom navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // Home navigation
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(
                    BookViewActivity.this,
                    MainActivity.class
            );
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        });

        // Explore navigation
        navExplore.setOnClickListener(v -> {
            Intent exploreIntent = new Intent(
                    BookViewActivity.this,
                    ExploreActivity.class
            );
            exploreIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(exploreIntent);
        });

        // Profile navigation
        navProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(
                    BookViewActivity.this,
                    ProfileActivity.class
            );
            startActivity(profileIntent);
        });
    }



    // Connect the reading list buttons to Firebase
    private void addBookToShelf(Book book, String readingList) {

        String listType = "to be read".equals(readingList)
                ? "to_be_read" : "currently_reading";

        View shelfButton = findViewById(R.id.addToShelfButton);
        View readingButton = findViewById(R.id.currentlyReadingButton);

        shelfButton.setEnabled(false);
        readingButton.setEnabled(false);

        saveBookToReadingList(book, listType)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            BookViewActivity.this,
                            "Book added to your " + readingList + " list",
                            Toast.LENGTH_SHORT
                    ).show();

                    if ("currently reading".equals(readingList)) {
                        Intent readingIntent = new Intent(
                                BookViewActivity.this,
                                CurrentReading.class
                        );

                        startActivity(readingIntent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            BookViewActivity.this,
                            "Could not save book: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnCompleteListener(task -> {
                    shelfButton.setEnabled(true);
                    readingButton.setEnabled(true);
                });
    }

    // Shared method for saving books from Book View and Search Results
    static Task<Void> saveBookToReadingList(Book book, String listType) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return Tasks.forException(new Exception("Please log in first"));
        }

        if (book.getId() == null || book.getId().trim().isEmpty()) {
            return Tasks.forException(new Exception("Book ID not available"));
        }

        if (!"to_be_read".equals(listType)
                && !"currently_reading".equals(listType)) {
            return Tasks.forException(new Exception("Invalid reading list"));
        }

        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return Tasks.forException(new Exception("Book title not available"));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        return findOrCreateBookReference(db, book)
                .onSuccessTask(bookRefId ->
                        findOrCreateReadingList(db, userId, listType)
                                .onSuccessTask(listId ->
                                        saveReadingEntry(
                                                db, userId, bookRefId,
                                                listId, listType
                                        )));
    }

    // Find or create the Google Books reference
    static Task<String> findOrCreateBookReference(
            FirebaseFirestore db, Book book) {

        String bookId = book.getId();
        String[] parts = bookId.split("\\|published:", 2);
        String externalBookId = parts[0];
        String publishedDate = parts.length > 1 ? parts[1] : "";

        if (externalBookId.trim().isEmpty() || externalBookId.contains("/")) {
            return Tasks.forException(new Exception("Invalid Google Books ID"));
        }

        return db.collection("bookReferences")
                .whereEqualTo("externalBookId", externalBookId)
                .whereEqualTo("bookSource", "google_books")
                .limit(1)
                .get(Source.SERVER)
                .onSuccessTask(documents -> {

                    if (!documents.isEmpty()) {
                        return Tasks.forResult(
                                documents.getDocuments().get(0).getId());
                    }

                    Map<String, Object> bookReference = new HashMap<>();

                    bookReference.put("externalBookId", externalBookId);
                    bookReference.put("bookSource", "google_books");
                    bookReference.put("title", book.getTitle());
                    bookReference.put("authors", book.getAuthors());
                    bookReference.put("description", book.getDescription());
                    bookReference.put("coverUrl", book.getCoverUrl());
                    bookReference.put("publishedDate", publishedDate);
                    bookReference.put("createdAt", FieldValue.serverTimestamp());

                    DocumentReference reference = db.collection("bookReferences")
                            .document("google_books_" + externalBookId);

                    return db.runTransaction(transaction -> {
                        DocumentSnapshot existing = transaction.get(reference);

                        if (!existing.exists()) {
                            transaction.set(reference, bookReference);
                        }

                        return reference.getId();
                    });
                });
    }

    // Find or create the user's reading list
    static Task<String> findOrCreateReadingList(
            FirebaseFirestore db, String userId, String listType) {

        return db.collection("readingLists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("listType", listType)
                .limit(1)
                .get(Source.SERVER)
                .onSuccessTask(documents -> {

                    if (!documents.isEmpty()) {
                        return Tasks.forResult(
                                documents.getDocuments().get(0).getId());
                    }

                    Map<String, Object> readingList = new HashMap<>();

                    readingList.put("userId", userId);
                    readingList.put("listType", listType);
                    readingList.put("createdAt", FieldValue.serverTimestamp());
                    readingList.put("updatedAt", FieldValue.serverTimestamp());

                    if ("to_be_read".equals(listType)) {
                        readingList.put("listName", "To Be Read");
                        readingList.put("description", "Books I want to read");
                    } else {
                        readingList.put("listName", "Currently Reading");
                        readingList.put("description", "Books I am currently reading");
                    }

                    DocumentReference reference = db.collection("readingLists")
                            .document(userId + "_" + listType);

                    return reference.set(readingList)
                            .onSuccessTask(unused ->
                                    Tasks.forResult(reference.getId()));
                });
    }

    // Save or move the book without resetting existing reading progress
    private static Task<Void> saveReadingEntry(
            FirebaseFirestore db, String userId, String bookRefId,
            String listId, String listType) {

        return db.collection("readingEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookRefId", bookRefId)
                .get(Source.SERVER)
                .onSuccessTask(documents -> {

                    if (documents.size() > 1) {
                        return Tasks.forException(new Exception(
                                "Duplicate reading entries found"));
                    }

                    if (!documents.isEmpty()) {

                        DocumentSnapshot document = documents.getDocuments().get(0);

                        if (Boolean.TRUE.equals(document.getBoolean("completed"))) {
                            return Tasks.forException(new Exception(
                                    "This book is already completed"));
                        }

                        Map<String, Object> updates = new HashMap<>();

                        updates.put("listId", listId);
                        updates.put("updatedAt", FieldValue.serverTimestamp());

                        if ("currently_reading".equals(listType)
                                && document.get("startedAt") == null) {
                            updates.put("startedAt", FieldValue.serverTimestamp());
                        }

                        return document.getReference().update(updates);
                    }

                    Map<String, Object> entry = new HashMap<>();

                    entry.put("userId", userId);
                    entry.put("bookRefId", bookRefId);
                    entry.put("listId", listId);
                    entry.put("progressPercent", 0);
                    entry.put("completed", false);
                    entry.put("addedAt", FieldValue.serverTimestamp());
                    entry.put("updatedAt", FieldValue.serverTimestamp());
                    entry.put("completedAt", null);

                    if ("currently_reading".equals(listType)) {
                        entry.put("startedAt", FieldValue.serverTimestamp());
                    } else {
                        entry.put("startedAt", null);
                    }

                    return db.collection("readingEntries")
                            .document(userId + "_" + bookRefId)
                            .set(entry);
                });
    }



    // Retrieve the selected book's reference from Firebase
    private void loadAppReviews() {

        LinearLayout reviewsContainer =
                findViewById(R.id.latestReviewsContainer);

        reviewsContainer.removeAllViews();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            displayReviewMessage("log in to view app reviews");
            return;
        }

        String bookId = getIntent().getStringExtra(EXTRA_ID);

        if (bookId == null || bookId.trim().isEmpty()) {
            displayReviewMessage("book ID not available");
            return;
        }

        String externalBookId = bookId.split("\\|published:", 2)[0];

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        displayReviewMessage("loading app reviews...");

        bookReferenceListener = db.collection("bookReferences")
                .whereEqualTo("externalBookId", externalBookId)
                .whereEqualTo("bookSource", "google_books")
                .limit(1)
                .addSnapshotListener((documents, error) -> {

                    if (reviewsListener != null) {
                        reviewsListener.remove();
                        reviewsListener = null;
                    }

                    if (error != null || documents == null) {
                        displayReviewMessage("could not load app reviews");
                        return;
                    }

                    if (documents.isEmpty()) {
                        displayReviewMessage("no app reviews yet");
                        return;
                    }

                    String bookRefId =
                            documents.getDocuments().get(0).getId();

                    retrieveAppReviews(db, bookRefId);
                });
    }

    // Retrieve the reviews associated with the selected book
    private void retrieveAppReviews(
            FirebaseFirestore db,
            String bookRefId) {

        reviewsListener = db.collection("reviews")
                .whereEqualTo("bookRefId", bookRefId)
                .addSnapshotListener((documents, error) -> {

                    if (error != null || documents == null) {
                        displayReviewMessage("could not load app reviews");
                        return;
                    }

                    ArrayList<DocumentSnapshot> reviews = new ArrayList<>();

                    double totalRating = 0;

                    for (DocumentSnapshot document : documents) {

                        Long rating = document.getLong("rating");

                        if (rating != null && rating >= 1 && rating <= 5) {

                            totalRating += rating;
                            reviews.add(document);
                        }
                    }

                    if (reviews.isEmpty()) {
                        displayReviewMessage("no app reviews yet");
                        return;
                    }

                    // Calculate the average rating
                    double average = totalRating / reviews.size();

                    hasAppRating = true;

                    TextView bookRating = findViewById(R.id.bookRating);

                    bookRating.setText(
                            String.format(
                                    Locale.UK,
                                    "★ %.1f / 5 (%d %s)",
                                    average,
                                    reviews.size(),
                                    reviews.size() == 1
                                            ? "app review"
                                            : "app reviews"
                            )
                    );

                    TextView averageRating =
                            findViewById(R.id.averageRating);

                    RatingBar averageRatingStars =
                            findViewById(R.id.averageRatingStars);

                    TextView reviewCount =
                            findViewById(R.id.reviewCount);

                    averageRating.setText(
                            String.format(Locale.UK, "%.1f", average));

                    averageRatingStars.setRating((float) average);

                    reviewCount.setText(
                            reviews.size() +
                                    (reviews.size() == 1
                                            ? " app review"
                                            : " app reviews")
                    );

                    // Sort the reviews with the newest first
                    reviews.sort((first, second) -> {

                        Timestamp firstDate =
                                first.getTimestamp("createdAt");

                        Timestamp secondDate =
                                second.getTimestamp("createdAt");

                        if (firstDate == null && secondDate == null) {
                            return 0;
                        }

                        if (firstDate == null) {
                            return 1;
                        }

                        if (secondDate == null) {
                            return -1;
                        }

                        return secondDate.compareTo(firstDate);
                    });

                    LinearLayout reviewsContainer =
                            findViewById(R.id.latestReviewsContainer);

                    reviewsContainer.removeAllViews();

                    // Display the five most recent reviews
                    for (int i = 0; i < Math.min(5, reviews.size()); i++) {

                        DocumentSnapshot review = reviews.get(i);

                        Long rating = review.getLong("rating");

                        String comment = review.getString("comment");

                        String userId = review.getString("userId");

                        String reviewer = "bookmark reader";

                        FirebaseUser currentUser =
                                FirebaseAuth.getInstance().getCurrentUser();

                        if (currentUser != null
                                && currentUser.getUid().equals(userId)) {

                            reviewer = "your review";
                        }

                        addReviewCard(
                                reviewsContainer,
                                reviewer,
                                rating.intValue(),
                                comment
                        );
                    }
                });
    }

    // Display a written review using the existing review design
    private void addReviewCard(
            LinearLayout container,
            String reviewer,
            int rating,
            String comment) {

        LinearLayout reviewCard = new LinearLayout(this);

        reviewCard.setOrientation(LinearLayout.HORIZONTAL);
        reviewCard.setGravity(android.view.Gravity.TOP);

        reviewCard.setBackgroundResource(
                R.drawable.profile_list_background);

        reviewCard.setPadding(
                reviewDp(10),
                reviewDp(10),
                reviewDp(10),
                reviewDp(10)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = reviewDp(8);

        // Display the reviewer's profile icon
        ImageView profileIcon = new ImageView(this);

        profileIcon.setImageResource(R.drawable.ic_profile);

        profileIcon.setBackgroundResource(
                R.drawable.book_back_background);

        profileIcon.setContentDescription("reviewer profile");

        profileIcon.setPadding(
                reviewDp(8),
                reviewDp(8),
                reviewDp(8),
                reviewDp(8)
        );

        reviewCard.addView(
                profileIcon,
                new LinearLayout.LayoutParams(
                        reviewDp(36),
                        reviewDp(36)
                )
        );

        LinearLayout reviewContent = new LinearLayout(this);

        reviewContent.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams contentParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        contentParams.setMarginStart(reviewDp(10));

        // Display the reviewer
        TextView reviewerName = new TextView(this);

        reviewerName.setText(reviewer);
        reviewerName.setTextColor(Color.parseColor("#44374D"));
        reviewerName.setTextSize(12);

        reviewContent.addView(reviewerName);

        // Display the selected star rating
        TextView reviewStars = new TextView(this);

        StringBuilder stars = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }

        reviewStars.setText(stars.toString());
        reviewStars.setTextColor(Color.parseColor("#AD94C0"));
        reviewStars.setTextSize(14);

        reviewContent.addView(reviewStars);

        // Display the written review
        TextView reviewComment = new TextView(this);

        reviewComment.setText(comment == null ? "" : comment);
        reviewComment.setTextColor(Color.parseColor("#666666"));
        reviewComment.setTextSize(12);

        LinearLayout.LayoutParams commentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        commentParams.topMargin = reviewDp(4);

        reviewContent.addView(reviewComment, commentParams);

        reviewCard.addView(reviewContent, contentParams);

        container.addView(reviewCard, cardParams);
    }

    // Display a message when reviews are unavailable
    private void displayReviewMessage(String message) {

        hasAppRating = false;

        TextView bookRating = findViewById(R.id.bookRating);

        bookRating.setText(googleBooksRating);

        TextView averageRating = findViewById(R.id.averageRating);

        RatingBar averageRatingStars =
                findViewById(R.id.averageRatingStars);

        TextView reviewCount = findViewById(R.id.reviewCount);

        LinearLayout reviewsContainer =
                findViewById(R.id.latestReviewsContainer);

        averageRating.setText("—");
        averageRatingStars.setRating(0);
        reviewCount.setText(message);

        reviewsContainer.removeAllViews();

        TextView reviewMessage = new TextView(this);

        reviewMessage.setText(message);
        reviewMessage.setTextColor(Color.parseColor("#777777"));
        reviewMessage.setTextSize(12);
        reviewMessage.setGravity(android.view.Gravity.CENTER);

        reviewsContainer.addView(reviewMessage);
    }

    // Convert display measurements to pixels
    private int reviewDp(int value) {

        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    // Stop listening to Firebase when Book View is closed
    private void stopAppReviews() {

        if (bookReferenceListener != null) {
            bookReferenceListener.remove();
            bookReferenceListener = null;
        }

        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }
    }


    // set up local mp3 playback for the five selected stephen king books
    private void setupAudioPlayer(String title, String authors) {

        LinearLayout audioContainer =
                findViewById(R.id.audioPlayerContainer);

        audioSeekBar = findViewById(R.id.audioSeekBar);
        audioPlayPauseButton = findViewById(R.id.audioPlayPauseButton);
        audioCurrentTime = findViewById(R.id.audioCurrentTime);
        audioTotalTime = findViewById(R.id.audioTotalTime);

        // connect the custom audio playback controls
        TextView rewindButton = findViewById(R.id.audioRewindButton);
        TextView forwardButton = findViewById(R.id.audioForwardButton);

        // check whether the book has one of the five custom synopses
        if (StephenKingSynopses.getSynopsis(title, authors) == null) {
            audioContainer.setVisibility(View.GONE);
            return;
        }

        // match the selected book to its locally stored mp3 file
        int audioResource = getStephenKingAudio(title);

        if (audioResource == 0) {
            audioContainer.setVisibility(View.GONE);
            return;
        }

        try {
            // create the media player using the selected audio file
            audioPlayer = MediaPlayer.create(this, audioResource);

            if (audioPlayer == null) {
                audioContainer.setVisibility(View.GONE);
                return;
            }

            audioContainer.setVisibility(View.VISIBLE);

            // display the total audio duration
            int duration = audioPlayer.getDuration();
            audioSeekBar.setMax(duration);
            audioTotalTime.setText(formatAudioTime(duration));
            audioCurrentTime.setText("0:00");

            // play or pause the narrated synopsis
            audioPlayPauseButton.setOnClickListener(v -> {

                if (audioPlayer == null) {
                    return;
                }

                if (audioPlayer.isPlaying()) {
                    audioPlayer.pause();
                    audioPlayPauseButton.setText("play");
                    audioHandler.removeCallbacks(audioProgressUpdater);

                } else {
                    audioPlayer.start();
                    audioPlayPauseButton.setText("pause");

                    audioHandler.removeCallbacks(audioProgressUpdater);
                    audioHandler.post(audioProgressUpdater);
                }
            });

            // rewind the audio by ten seconds
            rewindButton.setOnClickListener(v -> {

                if (audioPlayer != null) {
                    int position = Math.max(
                            0,
                            audioPlayer.getCurrentPosition() - 10000
                    );

                    seekAudioTo(position);
                }
            });

            // skip forward by ten seconds
            forwardButton.setOnClickListener(v -> {

                if (audioPlayer != null) {
                    int position = Math.min(
                            audioPlayer.getDuration(),
                            audioPlayer.getCurrentPosition() + 10000
                    );

                    seekAudioTo(position);
                }
            });

            // allow users to drag the playback progress bar
            audioSeekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onProgressChanged(
                                SeekBar seekBar,
                                int progress,
                                boolean fromUser) {

                            if (fromUser) {
                                audioCurrentTime.setText(
                                        formatAudioTime(progress)
                                );
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            seekAudioTo(seekBar.getProgress());
                        }
                    }
            );

            // reset the controls when the narration finishes
            audioPlayer.setOnCompletionListener(player -> {
                audioHandler.removeCallbacks(audioProgressUpdater);

                player.seekTo(0);
                audioSeekBar.setProgress(0);
                audioCurrentTime.setText("0:00");
                audioPlayPauseButton.setText("play");
            });

        } catch (Exception e) {
            android.util.Log.e(
                    "BookViewActivity",
                    "Could not initialise audio player",
                    e
            );

            audioHandler.removeCallbacks(audioProgressUpdater);

            if (audioPlayer != null) {
                audioPlayer.release();
                audioPlayer = null;
            }

            audioContainer.setVisibility(View.GONE);
        }
    }

    // identify the correct local audio file from the book title
    private int getStephenKingAudio(String title) {

        if (title == null) {
            return 0;
        }

        switch (normalise(title)) {

            case "theshining":
                return R.raw.the_shining;

            case "it":
                return R.raw.it;

            case "carrie":
                return R.raw.carrie;

            case "misery":
                return R.raw.misery;

            case "petsematary":
                return R.raw.pet_sematary;

            default:
                return 0;
        }
    }

    // seek to a selected position within the audio
    private void seekAudioTo(int position) {

        if (audioPlayer == null) {
            return;
        }

        audioPlayer.seekTo(position);
        audioSeekBar.setProgress(position);
        audioCurrentTime.setText(formatAudioTime(position));
    }

    // convert playback milliseconds into minutes and seconds
    private String formatAudioTime(int milliseconds) {

        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(
                Locale.UK,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    // retrieve ratings from the google books api
    private void loadBookRating(
            String title,
            String authors,
            TextView ratingView) {

        if (title == null || title.trim().isEmpty()) {
            ratingView.setText("not yet rated");
            return;
        }

        new Thread(() -> {

            HttpURLConnection connection = null;
            String ratingText = "not yet rated";

            try {

                // search for the selected book by title and author
                String query = "intitle:\"" + title + "\"";

                if (authors != null && !authors.trim().isEmpty()) {
                    String firstAuthor = authors.split(",")[0].trim();
                    query += " inauthor:\"" + firstAuthor + "\"";
                }

                String endpoint =
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&maxResults=20";

                connection = (HttpURLConnection)
                        new URL(endpoint).openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == 200) {

                    // read the api response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    // extract the book results
                    JSONObject json =
                            new JSONObject(response.toString());

                    JSONArray items = json.optJSONArray("items");

                    if (items != null) {

                        double bestRating = -1;
                        int bestCount = -1;

                        // find the matching edition with the most ratings
                        for (int i = 0; i < items.length(); i++) {

                            JSONObject volumeInfo = items
                                    .getJSONObject(i)
                                    .optJSONObject("volumeInfo");

                            if (volumeInfo == null
                                    || !volumeInfo.has("averageRating")) {
                                continue;
                            }

                            String resultTitle =
                                    volumeInfo.optString("title", "");

                            if (!normalise(resultTitle)
                                    .equals(normalise(title))) {
                                continue;
                            }

                            double rating = volumeInfo.optDouble(
                                    "averageRating", -1
                            );

                            int count = volumeInfo.optInt(
                                    "ratingsCount", 0
                            );

                            if (rating >= 0 && rating <= 5
                                    && count > bestCount) {
                                bestRating = rating;
                                bestCount = count;
                            }
                        }

                        // format the rating for display
                        if (bestRating >= 0) {
                            ratingText = String.format(
                                    Locale.UK,
                                    "★ %.1f / 5  (%d ratings)",
                                    bestRating,
                                    Math.max(bestCount, 0)
                            );
                        }
                    }
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "BookViewActivity",
                        "Could not load rating",
                        e
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // update the rating on the main thread
            String finalRatingText = ratingText;

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {

                    googleBooksRating = finalRatingText;

                    if (!hasAppRating) {
                        ratingView.setText(finalRatingText);
                    }
                }
            });

        }).start();
    }

    // normalise book titles for matching api results
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
                .replaceAll("[^a-z0-9]", "");
    }

    // update the book details colours without recreating the activity
    private void applyBookTheme(boolean dark) {
        darkThemeEnabled = dark;

        int page = Color.parseColor(dark ? "#211D27" : "#FFFCFE");
        int panel = Color.parseColor(dark ? "#302938" : "#F4EDF8");
        int primary = Color.parseColor(dark ? "#F5F0F8" : "#29232C");
        int secondary = Color.parseColor(dark ? "#D9CCDF" : "#4B4350");
        int accent = Color.parseColor(dark ? "#C9B1DB" : "#826B94");

        findViewById(R.id.bookViewRoot).setBackgroundColor(page);
        findViewById(R.id.bookViewContent).setBackgroundColor(page);

        // tint the existing rounded panels rather than replacing their shapes
        findViewById(R.id.synopsisPanel).setBackgroundTintList(
                ColorStateList.valueOf(panel));
        findViewById(R.id.audioPlayerContainer).setBackgroundTintList(
                ColorStateList.valueOf(panel));

        ((TextView) findViewById(R.id.backButton)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookTitle)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookAuthors)).setTextColor(secondary);
        ((TextView) findViewById(R.id.bookRating)).setTextColor(accent);
        ((TextView) findViewById(R.id.synopsisHeading)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookDescription)).setTextColor(secondary);
        ((TextView) findViewById(R.id.readMoreButton)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioHeading)).setTextColor(primary);
        ((TextView) findViewById(R.id.audioSubheading)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioCurrentTime)).setTextColor(secondary);
        ((TextView) findViewById(R.id.audioTotalTime)).setTextColor(secondary);
        ((TextView) findViewById(R.id.audioRewindButton)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioForwardButton)).setTextColor(accent);

        Log.d("AmbientLight", dark ? "Dark theme enabled" : "Light theme enabled");
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAppReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ambientLightHelper != null) {
            ambientLightHelper.start();
        }
    }

    @Override
    protected void onPause() {
        if (ambientLightHelper != null) {
            ambientLightHelper.stop();
        }
        super.onPause();
    }

    // pause the narration when the book screen is no longer visible
    @Override
    protected void onStop() {
        super.onStop();

        stopAppReviews();

        audioHandler.removeCallbacks(audioProgressUpdater);

        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();

            if (audioPlayPauseButton != null) {
                audioPlayPauseButton.setText("play");
            }
        }
    }

    // release the audio player when the activity is destroyed
    @Override
    protected void onDestroy() {

        audioHandler.removeCallbacks(audioProgressUpdater);

        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }

        super.onDestroy();
    }
}