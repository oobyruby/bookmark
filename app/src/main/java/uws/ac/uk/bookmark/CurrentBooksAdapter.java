package uws.ac.uk.bookmark;

// firebase
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

// create and display views
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//display book information
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageButton;
import android.widget.PopupMenu;

//RecyclerView components
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class CurrentBooksAdapter extends RecyclerView.Adapter<CurrentBooksAdapter.BookViewHolder> {

    // Stores the books that will appear in the list
    private List<CurrentBook> bookList;

    //holds the view belonging to one book card
    public static class BookViewHolder extends RecyclerView.ViewHolder {

        //Book card views
        ImageView bookCover;
        TextView bookTitle;
        TextView bookAuthor;
        ProgressBar bookProgress;
        TextView bookPercentage;
        ImageButton bookMenu;

        //Connect the java variable to the XML views
        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            bookCover = itemView.findViewById(R.id.currentBookCover);
            bookTitle = itemView.findViewById(R.id.currentBookTitle);
            bookAuthor = itemView.findViewById(R.id.currentBookAuthor);
            bookProgress = itemView.findViewById(R.id.currentBookProgress);
            bookPercentage = itemView.findViewById(R.id.currentBookPercentage);
            bookMenu = itemView.findViewById(R.id.currentBookMenu);
        }
    }

    //Receives the list of books from CurrentReading
    public CurrentBooksAdapter(List<CurrentBook> bookList) {
        this.bookList = bookList;
    }

    //Create a new book card
    @NonNull
    @Override

    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //load the book card XML layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_current_book, parent, false);
        //Returns the new book card
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {

        //get the book at this position in the list
        CurrentBook book = bookList.get(position);

        //Display the book information
        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());
        // Display the book cover
        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(R.drawable.genre_tile_background)
                .error(R.drawable.genre_tile_background)
                .into(holder.bookCover);

        //Display the reading process
        holder.bookProgress.setProgress(book.getProgress());
        holder.bookPercentage.setText(book.getProgress() + "% completed");

        holder.bookMenu.setOnClickListener(v -> {
            //create a menu with two options
            PopupMenu popupMenu = new PopupMenu(v.getContext(), holder.bookMenu);

            //Create the menu options
            popupMenu.getMenu().add("Update progress");
            popupMenu.getMenu().add("Delete book");

//Handle the selected menu option
            popupMenu.setOnMenuItemClickListener(item -> {

                if (item.getTitle().equals("Update progress")) {

                    //Show a dialog to enter the new progress
                    android.widget.EditText progressInput = new android.widget.EditText(v.getContext());
                    progressInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                    progressInput.setHint("Enter progress (0-100)");
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Update reading progress")
                            .setView(progressInput)
                            .setPositiveButton("Update", (dialog, which) -> {

                                //get the progress entered by the user
                                String value = progressInput.getText().toString();
                                //check that input is not empty
                                if (!value.isEmpty()) {
                                    // Convert the enter text into a number
                                    int newProgress = Integer.parseInt(value);

                                    //Make sure the progress is between 0 and 100
                                    if (newProgress >= 0 && newProgress <= 100) {

                                        //Update the book's progress in the app
                                        book.setProgress(newProgress);
                                        //update the progress bar displayed on screen
                                        holder.bookProgress.setProgress(newProgress);

                                        // updated the percentage text displayed on screen
                                        holder.bookPercentage.setText(newProgress + "% Completed");

                                        // connect to the firebase database
                                        FirebaseFirestore.getInstance()
                                                // open the collection
                                                .collection("readingEntries")
                                                // find the correct book using document it
                                                .document(book.getDocumentId())
                                                .update(
                                                        "progressPercent",newProgress, // save the new percentage
                                                        "completed", newProgress == 100,
                                                        "completedAt", newProgress == 100
                                                          ? FieldValue.serverTimestamp()// save when the book is completed
                                                        :null // clear the complete
                                                )
                                                //show a message in Logcat if the update succeeds
                                                .addOnSuccessListener(unused -> {
                                                    Log.d("CurrentBooks", "Progress save successfuly");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("CurrentBooks", "could not save progress",e);
                                                });
                                    }

                                }
                            })
                            //add the cancel button
                            .setNegativeButton("Cancel", null)
                            //display the dialog
                            .show();

                }

                // If the user selects Delete book, handle it here
                else if (item.getTitle().equals("Delete book")) {

                    //ask the user to confirm before deleting
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Delete book")
                            .setMessage("Are you sure you want to delete this book ?")

                            //delete the book if the user confirms
                            .setPositiveButton("Delete", (dialog, which)->{
                                //connect to the firebase database
                                FirebaseFirestore.getInstance()

                                        //open the readingEntites
                                        .collection("readingEntries")
                                        //find the selected book using its document id
                                        .document(book.getDocumentId())
                                        //Delete the book from firebase
                                        .delete()
                                        //check if the deletion worked
                                        .addOnSuccessListener( unused -> {
                                            int currentPosition = holder.getBindingAdapterPosition();

                                            if (currentPosition != RecyclerView.NO_POSITION){
                                                //refresh the RecyclerView
                                                bookList.remove(currentPosition);
                                                notifyItemRemoved(currentPosition);
                                            }
                                            //show success message on Logcat
                                            Log.d("CurrentBooks", "Book deleted successfully");
                                        })
                                        //handle deletion errors
                                        .addOnFailureListener(e -> {
                                            Log.e("CurrentBooks", "Could not deleted book",e);
                                        });
                            })
                            //cancel deletion if the user change their mind
                            .setNegativeButton("Cancel", null)
                            //Display the conformation dialog
                    .show();
                }

                // Finish handling the selected menu option
                return true;
            });

            // Display the two-option menu
            popupMenu.show();
        });
    }
@Override
//return the number of books
public  int getItemCount(){
    return bookList.size();
    }
}