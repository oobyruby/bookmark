
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

import java.util.ArrayList;

import java.util.Locale;


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

        // Display the bookmark icon
        holder.bookBookmark.setImageResource(
                R.drawable.bookmark_empty);
        holder.bookBookmark.setSelected(false);

        // Handle the bookmark button
        holder.bookBookmark.setOnClickListener(v -> {

            boolean bookmarked = !v.isSelected();
            v.setSelected(bookmarked);

            holder.bookBookmark.setImageResource(
                    bookmarked
                            ? R.drawable.bookmark
                            : R.drawable.bookmark_empty
            );

            // Placeholder for connecting bookmarks to Firebase :)
        });

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
                            book.getId(),
                            rating,
                            review
                    );
                });
    }

    // Placeholder for connecting reviews to Firebase :D
    private static void submitReview(
            View view,
            String bookId,
            float rating,
            String review) {

        Toast.makeText(
                view.getContext(),
                "review not saved - database connection pending",
                Toast.LENGTH_SHORT
        ).show();
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