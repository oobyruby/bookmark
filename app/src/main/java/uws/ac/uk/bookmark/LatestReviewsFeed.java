package uws.ac.uk.bookmark;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LatestReviewsFeed {

    private final AppCompatActivity activity;
    private final LinearLayout container;
    private final FirebaseFirestore db;
    private final GoogleBooksService googleBooksService;

    private ListenerRegistration reviewsListener;
    private boolean darkMode = false;
    private boolean active = false;
    private boolean previewOnly = false;

    public LatestReviewsFeed(
            AppCompatActivity activity,
            LinearLayout container) {

        this.activity = activity;
        this.container = container;

        db = FirebaseFirestore.getInstance();
        googleBooksService = new GoogleBooksService(activity);
    }

    // retrieve the latest reviews from firebase
    public void loadReviews(int limit) {

        stopListening();

        active = true;
        previewOnly = limit > 0;

        displayMessage("loading latest reviews...");

        Query query = db.collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        // limit the number of reviews on the homepage
        if (limit > 0) {
            query = query.limit(limit);
        }

        reviewsListener = query.addSnapshotListener((documents, error) -> {

            if (!active || activity.isDestroyed()) {
                return;
            }

            if (error != null || documents == null) {

                Log.e("LatestReviews", "could not retrieve reviews", error);

                displayMessage("could not load latest reviews");
                return;
            }

            container.removeAllViews();

            if (documents.isEmpty()) {
                displayMessage("no reviews yet");
                return;
            }

            // display each review in chronological order
            for (DocumentSnapshot review : documents) {
                addReviewCard(review);
            }
        });
    }

    // create and populate an individual review card
    private void addReviewCard(DocumentSnapshot review) {

        View card = LayoutInflater.from(activity).inflate(
                R.layout.item_home_review,
                container,
                false
        );

        TextView username = card.findViewById(R.id.reviewUsername);
        TextView stars = card.findViewById(R.id.reviewStars);
        TextView date = card.findViewById(R.id.reviewDate);
        TextView bookTitle = card.findViewById(R.id.reviewBookTitle);
        TextView comment = card.findViewById(R.id.reviewComment);

        ImageView profileImage =
                card.findViewById(R.id.reviewProfileImage);

        ImageView bookCover =
                card.findViewById(R.id.reviewBookCover);

        String userId = review.getString("userId");
        String bookRefId = review.getString("bookRefId");
        String reviewComment = review.getString("comment");

        Long rating = review.getLong("rating");

        Boolean containsSpoilers =
                review.getBoolean("containsSpoilers");

        username.setText("bookmark reader");
        bookTitle.setText("book unavailable");

        // display the star rating
        StringBuilder ratingStars = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            ratingStars.append(
                    rating != null && i <= rating ? "★" : "☆"
            );
        }

        stars.setText(ratingStars.toString());

        // format the date of the review
        Timestamp timestamp = review.getTimestamp("createdAt");

        if (timestamp != null) {

            SimpleDateFormat formatter =
                    new SimpleDateFormat("dd MMM yyyy", Locale.UK);

            date.setText(formatter.format(timestamp.toDate()));
        }

        // display the full comment on the see all page
        if (!previewOnly) {
            comment.setMaxLines(Integer.MAX_VALUE);
            comment.setEllipsize(null);
        }

        String finalComment = reviewComment == null
                || reviewComment.trim().isEmpty()
                ? "rating only"
                : reviewComment;

        // hide comments containing spoilers
        if (Boolean.TRUE.equals(containsSpoilers)) {

            comment.setText("contains spoilers • tap to reveal");

            comment.setOnClickListener(v -> {

                comment.setText(finalComment);

                comment.setOnClickListener(null);
            });

        } else {
            comment.setText(finalComment);
        }

        container.addView(card);

        // apply the current light or dark theme
        applyCardTheme(card);

        // retrieve the reviewer's profile
        if (userId != null && !userId.isEmpty()
                && !userId.contains("/")) {

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(user -> {

                        if (!isCardActive(card) || !user.exists()) {
                            return;
                        }

                        String name = user.getString("username");
                        String photoUrl = user.getString("photoUrl");

                        if (name != null && !name.isEmpty()) {
                            username.setText(name);
                        }

                        if (photoUrl != null && !photoUrl.isEmpty()) {

                            Glide.with(activity)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .into(profileImage);
                        }
                    })
                    .addOnFailureListener(error ->
                            Log.e("LatestReviews",
                                    "could not retrieve reviewer", error));
        }

        // retrieve the book associated with the review
        if (bookRefId != null && !bookRefId.isEmpty()
                && !bookRefId.contains("/")) {

            db.collection("bookReferences")
                    .document(bookRefId)
                    .get()
                    .addOnSuccessListener(book -> {

                        if (!isCardActive(card) || !book.exists()) {
                            return;
                        }

                        String title = book.getString("title");
                        String authors = book.getString("authors");
                        String coverUrl = book.getString("coverUrl");
                        String externalBookId =
                                book.getString("externalBookId");

                        String description =
                                book.getString("description");

                        if (title != null && !title.isEmpty()) {
                            bookTitle.setText(title);
                        }

                        // use the existing cover from google books or nyt
                        if (coverUrl != null && !coverUrl.trim().isEmpty()) {

                            displayBookCover(bookCover, coverUrl);

                            setupBookClick(
                                    bookCover,
                                    externalBookId,
                                    title,
                                    authors,
                                    description,
                                    coverUrl
                            );

                        } else {

                            // retrieve a cover from google books if missing
                            retrieveBookCover(
                                    card,
                                    bookCover,
                                    externalBookId,
                                    title,
                                    authors,
                                    description
                            );
                        }
                    })
                    .addOnFailureListener(error ->
                            Log.e("LatestReviews",
                                    "could not retrieve book", error));
        }
    }

    // retrieve a missing book cover using the google books api
    private void retrieveBookCover(
            View card,
            ImageView bookCover,
            String externalBookId,
            String title,
            String authors,
            String description) {

        if (title == null || title.trim().isEmpty()) {
            return;
        }

        setupBookClick(
                bookCover,
                externalBookId,
                title,
                authors,
                description,
                ""
        );

        String query = "intitle:\"" + title + "\"";

        if (authors != null && !authors.trim().isEmpty()) {

            query += " inauthor:\"" + authors + "\"";
        }

        googleBooksService.getBooks(
                query,
                new GoogleBooksService.BooksCallback() {

                    @Override
                    public void onSuccess(ArrayList<Book> books) {

                        activity.runOnUiThread(() -> {

                            if (!isCardActive(card)) {
                                return;
                            }

                            Book matchingBook = null;

                            // prefer the original google books edition
                            for (Book book : books) {

                                if (book == null || book.getId() == null) {
                                    continue;
                                }

                                String resultId = book.getId()
                                        .split("\\|published:", 2)[0];

                                if (resultId.equals(externalBookId)
                                        && hasCover(book)) {

                                    matchingBook = book;
                                    break;
                                }
                            }

                            // otherwise match the title and author
                            if (matchingBook == null) {

                                for (Book book : books) {

                                    if (book == null) {
                                        continue;
                                    }

                                    boolean titleMatches =
                                            normalise(book.getTitle())
                                                    .equals(normalise(title));

                                    boolean authorMatches =
                                            authors == null
                                                    || authors.trim().isEmpty()
                                                    || normalise(book.getAuthors())
                                                    .contains(normalise(authors));

                                    if (titleMatches
                                            && authorMatches
                                            && hasCover(book)) {

                                        matchingBook = book;
                                        break;
                                    }
                                }
                            }

                            if (matchingBook != null) {

                                displayBookCover(
                                        bookCover,
                                        matchingBook.getCoverUrl()
                                );

                                setupBookClick(
                                        bookCover,
                                        externalBookId,
                                        title,
                                        authors,
                                        description,
                                        matchingBook.getCoverUrl()
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {

                        Log.e("LatestReviews",
                                "could not retrieve book cover: " + message);
                    }
                });
    }

    // check whether a book has a cover image
    private boolean hasCover(Book book) {

        return book.getCoverUrl() != null
                && !book.getCoverUrl().trim().isEmpty();
    }

    // normalise titles and authors for matching
    private String normalise(String value) {

        if (value == null) {
            return "";
        }

        return java.text.Normalizer.normalize(
                        value,
                        java.text.Normalizer.Form.NFD
                ).toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "");
    }

    // display the book cover using glide
    private void displayBookCover(
            ImageView bookCover,
            String coverUrl) {

        Glide.with(activity)
                .load(coverUrl)
                .fitCenter()
                .placeholder(R.drawable.genre_tile_background)
                .error(R.drawable.genre_tile_background)
                .into(bookCover);
    }

    // open the reviewed book when its cover is selected
    private void setupBookClick(
            ImageView bookCover,
            String bookId,
            String title,
            String authors,
            String description,
            String coverUrl) {

        if (bookId == null || bookId.trim().isEmpty()) {
            return;
        }

        bookCover.setOnClickListener(v -> {

            Intent intent = new Intent(
                    activity,
                    BookViewActivity.class
            );

            intent.putExtra(BookViewActivity.EXTRA_ID, bookId);
            intent.putExtra(BookViewActivity.EXTRA_TITLE, title);
            intent.putExtra(BookViewActivity.EXTRA_AUTHORS, authors);

            intent.putExtra(
                    BookViewActivity.EXTRA_DESCRIPTION,
                    description
            );

            intent.putExtra(
                    BookViewActivity.EXTRA_COVER_URL,
                    coverUrl
            );

            activity.startActivity(intent);
        });
    }

    // check that the review card is still being displayed
    private boolean isCardActive(View card) {

        return active
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && card.getParent() == container;
    }

    // display a loading, empty or error message
    private void displayMessage(String message) {

        container.removeAllViews();

        TextView textView = new TextView(activity);

        textView.setText(message);
        textView.setTextSize(12);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(0, 20, 0, 20);

        textView.setTextColor(Color.parseColor(
                darkMode ? "#C4B5CF" : "#8A7B92"
        ));

        container.addView(textView);
    }

    // update review cards when the ambient lighting changes
    public void setDarkMode(boolean dark) {

        darkMode = dark;

        for (int i = 0; i < container.getChildCount(); i++) {

            View child = container.getChildAt(i);

            if (child instanceof TextView) {

                ((TextView) child).setTextColor(Color.parseColor(
                        dark ? "#C4B5CF" : "#8A7B92"
                ));

            } else {
                applyCardTheme(child);
            }
        }
    }

    // apply the current theme to an individual review card
    private void applyCardTheme(View card) {

        if (darkMode) {

            GradientDrawable background = new GradientDrawable();

            background.setColor(Color.parseColor("#302938"));

            background.setCornerRadius(
                    12 * activity.getResources()
                            .getDisplayMetrics().density
            );

            background.setStroke(
                    1,
                    Color.parseColor("#69517D")
            );

            card.setBackground(background);

        } else {

            card.setBackgroundResource(
                    R.drawable.profile_list_background
            );
        }

        setTextColour(card, R.id.reviewUsername,
                darkMode ? "#F0E7F6" : "#44374D");

        setTextColour(card, R.id.reviewComment,
                darkMode ? "#F0E7F6" : "#666666");

        setTextColour(card, R.id.reviewDate,
                darkMode ? "#C4B5CF" : "#888888");

        setTextColour(card, R.id.reviewBookTitle,
                darkMode ? "#C4B5CF" : "#826B94");

        setTextColour(card, R.id.reviewStars, "#AD94C0");
    }

    private void setTextColour(View card, int id, String colour) {

        TextView textView = card.findViewById(id);

        if (textView != null) {
            textView.setTextColor(Color.parseColor(colour));
        }
    }

    // stop listening when the activity becomes inactive
    public void stopListening() {

        active = false;

        if (reviewsListener != null) {

            reviewsListener.remove();
            reviewsListener = null;
        }
    }

    // release the google books service
    public void shutdown() {

        stopListening();
        googleBooksService.shutdown();
    }
}