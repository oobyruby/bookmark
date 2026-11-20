
package uws.ac.uk.bookmark;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RatingBar;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;


public class SearchResultsAdapter
        extends RecyclerView.Adapter<SearchResultsAdapter.BookViewHolder> {

    private final ArrayList<Book> books = new ArrayList<>();

    // Update the list when books are received from the API
    public void setBooks(ArrayList<Book> newBooks) {
        books.clear();
        books.addAll(newBooks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);

        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BookViewHolder holder, int position) {

        Book book = books.get(position);

        // Display the book title

        holder.bookTitle.setText(
                book.getTitle() == null
                        ? ""
                        : book.getTitle().toLowerCase(Locale.ROOT)
        );

        // Display the author
        String authors = book.getAuthors();

        if (authors != null && !authors.isEmpty()) {
            holder.bookAuthors.setText(authors);
        } else {
            holder.bookAuthors.setText(R.string.author_not_available);
        }

        // Display the book cover using Glide
        if (book.getCoverUrl() != null &&
                !book.getCoverUrl().isEmpty()) {

            Glide.with(holder.itemView)
                    .load(book.getCoverUrl())
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(holder.bookCover);

        } else {
            Glide.with(holder.itemView).clear(holder.bookCover);
            holder.bookCover.setImageResource(
                    R.drawable.genre_tile_background);
        }

        // Reset the stars when a search result is reused
        holder.bookRating.setOnRatingBarChangeListener(null);
        holder.bookRating.setRating(0);

        // Open the review dialog when a star rating is selected
        holder.bookRating.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    if (fromUser && rating > 0) {
                        showReviewDialog(
                                holder.itemView,
                                book,
                                rating
                        );

                        ratingBar.setRating(0);
                    }
                });

        // Display and connect the bookmark icon
        bindBookmark(holder, book);

        // Open Book View when a book is selected
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    v.getContext(),
                    BookViewActivity.class
            );

            // Pass the selected book's ID
            intent.putExtra(
                    "book_id",
                    book.getId()
            );

            // Pass the selected book's title
            intent.putExtra(
                    BookViewActivity.EXTRA_TITLE,
                    book.getTitle()
            );

            // Pass the selected book's author
            intent.putExtra(
                    BookViewActivity.EXTRA_AUTHORS,
                    book.getAuthors()
            );

            // Pass the selected book's cover URL
            intent.putExtra(
                    BookViewActivity.EXTRA_COVER_URL,
                    book.getCoverUrl()
            );

            // Pass the selected book's description
            intent.putExtra(
                    BookViewActivity.EXTRA_DESCRIPTION,
                    book.getDescription()
            );

            // Open Screen bookoverview
            v.getContext().startActivity(intent);
        });
    }

    // Retrieve and display the saved bookmark
    private void bindBookmark(BookViewHolder holder, Book book) {

        Object bookmarkTag = new Object();

        holder.bookBookmark.setTag(bookmarkTag);
        holder.bookBookmark.setSelected(false);
        holder.bookBookmark.setImageResource(
                R.drawable.bookmark_empty);

        holder.bookBookmark.setOnClickListener(null);
        holder.bookBookmark.setEnabled(false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            holder.bookBookmark.setEnabled(true);

            holder.bookBookmark.setOnClickListener(v ->
                    Toast.makeText(
                            v.getContext(),
                            "Please log in first",
                            Toast.LENGTH_SHORT
                    ).show());

            return;
        }

        findBookmark(book)
                .addOnSuccessListener(document -> {

                    if (holder.bookBookmark.getTag() != bookmarkTag) {
                        return;
                    }

                    boolean bookmarked = document != null;

                    holder.bookBookmark.setSelected(bookmarked);

                    holder.bookBookmark.setImageResource(
                            bookmarked
                                    ? R.drawable.bookmark
                                    : R.drawable.bookmark_empty
                    );

                    holder.bookBookmark.setEnabled(true);

                    // Handle the bookmark button
                    holder.bookBookmark.setOnClickListener(v -> {

                        v.setEnabled(false);

                        boolean remove = v.isSelected();

                        Task<Void> task;

                        if (remove) {
                            task = removeBookmark(book);
                        } else {
                            task = BookViewActivity.saveBookToReadingList(
                                    book,
                                    "to_be_read"
                            );
                        }

                        task.addOnSuccessListener(unused ->
                                Toast.makeText(
                                        v.getContext(),
                                        remove
                                                ? "Bookmark removed"
                                                : "Book added to your to be read list",
                                        Toast.LENGTH_SHORT
                                ).show());

                        task.addOnFailureListener(e ->
                                Toast.makeText(
                                        v.getContext(),
                                        "Could not update bookmark: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show());

                        task.addOnCompleteListener(result -> {

                            if (holder.bookBookmark.getTag() == bookmarkTag) {
                                bindBookmark(holder, book);
                            }
                        });
                    });
                })
                .addOnFailureListener(e ->
                        android.util.Log.e(
                                "SearchResultsAdapter",
                                "Could not retrieve bookmark",
                                e
                        ));
    }

    // Find the selected book in the user's to be read list
    private Task<DocumentSnapshot> findBookmark(Book book) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return Tasks.forException(
                    new Exception("Please log in first"));
        }

        if (book.getId() == null || book.getId().trim().isEmpty()) {
            return Tasks.forException(
                    new Exception("Book ID not available"));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = user.getUid();
        String externalBookId = book.getId().split("\\|published:", 2)[0];

        return db.collection("readingLists")
                .whereEqualTo("userId", userId)
                .whereEqualTo("listType", "to_be_read")
                .limit(1)
                .get(Source.SERVER)
                .onSuccessTask(lists -> {

                    if (lists.isEmpty()) {
                        return Tasks.forResult((DocumentSnapshot) null);
                    }

                    String listId =
                            lists.getDocuments().get(0).getId();

                    return db.collection("bookReferences")
                            .whereEqualTo("externalBookId", externalBookId)
                            .whereEqualTo("bookSource", "google_books")
                            .limit(1)
                            .get(Source.SERVER)
                            .onSuccessTask(references -> {

                                if (references.isEmpty()) {
                                    return Tasks.forResult(
                                            (DocumentSnapshot) null);
                                }

                                String bookRefId =
                                        references.getDocuments().get(0).getId();

                                return db.collection("readingEntries")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("listId", listId)
                                        .whereEqualTo("bookRefId", bookRefId)
                                        .whereEqualTo("completed", false)
                                        .limit(1)
                                        .get(Source.SERVER)
                                        .onSuccessTask(entries ->
                                                Tasks.forResult(
                                                        entries.isEmpty()
                                                                ? null
                                                                : entries.getDocuments().get(0)
                                                ));
                            });
                });
    }

    // Remove the selected book from the to be read list
    private Task<Void> removeBookmark(Book book) {

        return findBookmark(book)
                .onSuccessTask(document -> {

                    if (document == null) {
                        return Tasks.forResult((Void) null);
                    }

                    FirebaseFirestore db =
                            FirebaseFirestore.getInstance();

                    String listId = document.getString("listId");

                    return db.runTransaction(transaction -> {

                        DocumentSnapshot current =
                                transaction.get(document.getReference());

                        if (current.exists()
                                && listId.equals(current.getString("listId"))
                                && Boolean.FALSE.equals(
                                current.getBoolean("completed"))) {

                            transaction.delete(document.getReference());
                        }

                        return null;
                    });
                });
    }

    // Display the review dialog for the selected book
    static void showReviewDialog(
            View view,
            Book book,
            float selectedRating) {

        LinearLayout reviewLayout =
                new LinearLayout(view.getContext());

        reviewLayout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 *
                view.getResources().getDisplayMetrics().density);

        reviewLayout.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        // Display the selected rating and allow the user to change it
        RatingBar reviewRating =
                new RatingBar(view.getContext());

        reviewRating.setNumStars(5);
        reviewRating.setStepSize(1);
        reviewRating.setRating(selectedRating);
        reviewRating.setIsIndicator(false);

        reviewRating.setProgressTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#AD94C0")
                )
        );

        // Set the colour of the unselected stars
        reviewRating.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E5D8E9")
                )
        );

        reviewRating.setSecondaryProgressTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E5D8E9")
                )
        );

        // Centre the stars within the review dialog
        LinearLayout.LayoutParams ratingParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        ratingParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        ratingParams.bottomMargin = padding;

        reviewLayout.addView(
                reviewRating,
                ratingParams
        );

        // Allow the user to enter a written review
        EditText reviewInput =
                new EditText(view.getContext());

        reviewInput.setHint("write your review here...");
        reviewInput.setGravity(android.view.Gravity.TOP);

        reviewInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        reviewInput.setMinLines(3);

        // Styled the review input
        reviewInput.setBackgroundResource(
                R.drawable.profile_list_background);

        reviewInput.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        reviewInput.setTextColor(
                android.graphics.Color.parseColor("#44374D"));

        reviewInput.setHintTextColor(
                android.graphics.Color.parseColor("#88818D"));

        reviewInput.setTextSize(14);

        reviewLayout.addView(
                reviewInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(view.getContext())
                        .setTitle("write a review")
                        .setMessage(book.getTitle())
                        .setView(reviewLayout)
                        .setNegativeButton("cancel", null)
                        .setPositiveButton("submit review", null)
                        .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(
                        android.graphics.Color.parseColor("#826B94")
                );

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(
                        android.graphics.Color.parseColor("#826B94")
                );

        // Retrieve the rating and review when the user submits
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {

                    String review =
                            reviewInput.getText().toString().trim();

                    float rating = reviewRating.getRating();

                    if (rating == 0) {
                        Toast.makeText(
                                view.getContext(),
                                "please select a rating",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    if (review.isEmpty()) {
                        reviewInput.setError("please enter a review");
                        return;
                    }

                    submitReview(
                            view,
                            book,
                            rating,
                            review,
                            dialog
                    );
                });
    }

    // Connect reviews to Firebase
    private static void submitReview(
            View view,
            Book book,
            float rating,
            String review,
            AlertDialog dialog) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    view.getContext(),
                    "please log in first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (book.getId() == null || book.getId().trim().isEmpty()) {
            Toast.makeText(
                    view.getContext(),
                    "book ID not available",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            Toast.makeText(
                    view.getContext(),
                    "book title not available",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = user.getUid();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(false);

        BookViewActivity.findOrCreateBookReference(db, book)
                .onSuccessTask(bookRefId ->
                        saveReview(
                                db,
                                userId,
                                bookRefId,
                                (int) rating,
                                review
                        ))
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            view.getContext(),
                            "review saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            view.getContext(),
                            "could not save review: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnCompleteListener(task -> {

                    if (dialog.isShowing()) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(true);
                    }
                });
    }

    // Save or update the user's review
    private static Task<Void> saveReview(
            FirebaseFirestore db,
            String userId,
            String bookRefId,
            int rating,
            String review) {

        return db.collection("reviews")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookRefId", bookRefId)
                .get(Source.SERVER)
                .onSuccessTask(documents -> {

                    if (documents.size() > 1) {
                        return Tasks.forException(
                                new Exception("duplicate reviews found"));
                    }

                    DocumentReference reference;

                    if (!documents.isEmpty()) {
                        reference = documents.getDocuments()
                                .get(0).getReference();
                    } else {
                        reference = db.collection("reviews")
                                .document(userId + "_" + bookRefId);
                    }

                    return db.runTransaction(transaction -> {

                        DocumentSnapshot existing =
                                transaction.get(reference);

                        Map<String, Object> reviewData =
                                new HashMap<>();

                        reviewData.put("userId", userId);
                        reviewData.put("bookRefId", bookRefId);
                        reviewData.put("rating", rating);
                        reviewData.put("comment", review);
                        reviewData.put("updatedAt",
                                FieldValue.serverTimestamp());

                        if (existing.exists()) {

                            reviewData.put("containsSpoilers",
                                    Boolean.TRUE.equals(
                                            existing.getBoolean("containsSpoilers")));

                            transaction.update(reference, reviewData);

                        } else {

                            reviewData.put("containsSpoilers", false);
                            reviewData.put("createdAt",
                                    FieldValue.serverTimestamp());

                            transaction.set(reference, reviewData);
                        }

                        return null;
                    });
                });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;
        TextView bookTitle;
        TextView bookAuthors;
        RatingBar bookRating;
        ImageView bookBookmark;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);

            bookCover = itemView.findViewById(R.id.resultBookCover);
            bookTitle = itemView.findViewById(R.id.resultBookTitle);
            bookAuthors = itemView.findViewById(R.id.resultBookAuthors);
            bookRating = itemView.findViewById(R.id.resultBookRating);
            bookBookmark = itemView.findViewById(R.id.resultBookmark);
        }
    }
}
