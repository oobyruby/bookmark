package uws.ac.uk.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import java.util.List;

import com.bumptech.glide.Glide;
public class CompletedBooksAdapter extends RecyclerView.Adapter<CompletedBooksAdapter.BookViewHolder> {
    //store completed books
    private List<CurrentBook> bookList;

    //connect the XML view
    public static class BookViewHolder extends RecyclerView.ViewHolder{

        ImageView bookCover;
        TextView bookTitle;
        TextView bookAuthor;
        ImageButton bookMenu;

        public BookViewHolder(@NonNull View itemView){
            super(itemView);

            bookCover = itemView.findViewById(R.id.completedBookCover);
            bookTitle = itemView.findViewById(R.id.completedBookTitle);
            bookAuthor = itemView.findViewById(R.id.completedBookAuthor);
            bookMenu = itemView.findViewById(R.id.completedBookMenu);
        }
    }
    //Recieve the complete book list
    public CompletedBooksAdapter(List<CurrentBook> bookList){
        this.bookList = bookList;
    }
    // Create each completed book row
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_book, parent, false);
        return new BookViewHolder(view);
    }
    //Display book information

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {

        // Get the book at this position
        CurrentBook book = bookList.get(position);

        // Display book information
        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());

        // Load the book cover
        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(R.drawable.genre_tile_background)
                .into(holder.bookCover);

        // Set up the three-dot menu
        holder.bookMenu.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(v.getContext(), holder.bookMenu);

            popupMenu.getMenu().add("Write a review");
            popupMenu.getMenu().add("Delete book");

            //Handle menu option clicks
            popupMenu.setOnMenuItemClickListener(item -> {
                //check if te users want write a review
                if (item.getTitle().equals("Write a review")) {
                    if (book.getExternalBookId() == null ||
                            book.getExternalBookId().trim().isEmpty()) {
                        //tell the user if the book ID is missing
                        Toast.makeText(v.getContext(), "Book ID not available", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    //convert the CurrentBOok object into a Book Object
                    Book reviewBook = new Book(
                            book.getExternalBookId(),
                            book.getTitle(),
                            book.getAuthor(),
                            null,
                            book.getCoverUrl()
                    );
                    //open the review dialog from searchResultAdapter
                    SearchResultsAdapter.showReviewDialog(
                            holder.itemView,
                            reviewBook,
                            0
                    );
                    return true;
                }
                // Check if the user wants to delete the completed book
                if (item.getTitle().equals("Delete book")) {

                    // Ask the user to confirm deletion
                    new androidx.appcompat.app.AlertDialog.Builder(holder.itemView.getContext())
                            .setTitle("Delete book")
                            .setMessage("Are you sure you want to remove this book from your completed list?")
                            .setPositiveButton("Delete", (dialog, which) -> {

                                // Delete the reading entry from Firestore
                                FirebaseFirestore.getInstance()
                                        .collection("readingEntries")
                                        .document(book.getDocumentId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {

                                            // Remove the book from the displayed list
                                            int currentPosition = bookList.indexOf(book);

                                            if (currentPosition != -1) {
                                                bookList.remove(currentPosition);
                                                notifyItemRemoved(currentPosition);
                                            }

                                            // Show a confirmation message
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Book removed from completed list",
                                                    Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {

                                            // Show an error message
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Could not delete book: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });

                            })
                            .setNegativeButton("Cancel", null)
                            .show();

                    return true;
                }

                return false;
            });

// Show the three-dot menu
            popupMenu.show();
        });
    } // End of onBindViewHolder

    // Return the number of completed books
    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
