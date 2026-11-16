package uws.ac.uk.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
public class ToBeReadAdapter extends RecyclerView.Adapter<ToBeReadAdapter.BookViewHolder> {

    //Stores the books that will appear on scren
    private List<CurrentBook> bookList;

    //connect the adapter to the book list
    public ToBeReadAdapter(List<CurrentBook> bookList) {
        this.bookList = bookList;


    }

    // holds the view for each book card
    public static class BookViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;
        TextView bookTitle;
        TextView bookAuthor;
        ImageButton bookMenu;
        // Holds the favourite star image
        ImageView bookFavourite;
        //Constructor that connects the java variable to the xml
        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find each view using its ID from item_current_book.xml
            // Connect to the views in item_to_be_read.xml
            bookCover = itemView.findViewById(R.id.toBeReadBookCover);
            bookTitle = itemView.findViewById(R.id.toBeReadBookTitle);
            bookAuthor = itemView.findViewById(R.id.toBeReadBookAuthor);
            // Connect to the three-dot menu
            bookMenu = itemView.findViewById(R.id.toBeReadMenu);
            // connect to the favourite image
            bookFavourite = itemView.findViewById(R.id.toBeReadFavourite);
        }
    }
    //create each book card using separate layout
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
       //load the item_to_be_read.xml
       View view = LayoutInflater.from(parent.getContext())
               .inflate(R.layout.item_to_be_read, parent,false);
       // return new book card
        return new BookViewHolder(view);
    }
    // display the details of each book in the card
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position){
        //get the book at the current position in the list
        CurrentBook book = bookList.get(position);
        //display the book title
        holder.bookTitle.setText(book.getTitle());
        holder.bookAuthor.setText(book.getAuthor());
        //get the book cover image url
        String coverUrl = book.getCoverUrl();

        //Load the cover if a url is available
        if (coverUrl != null && !coverUrl.isEmpty()){
            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(holder.bookCover);
        } else{
            //show a placeholder if there is no cover url
            Glide.with(holder.itemView.getContext()).clear(holder.bookCover);
            holder.bookCover.setImageResource(R.drawable.genre_tile_background);
        }
        //display the filled bookmark as it in to be read
        holder.bookFavourite.setImageResource(R.drawable.bookmark);
        holder.bookFavourite.setOnClickListener(v->{
            //remove the book from display list
            FirebaseFirestore.getInstance()
                    .collection("readingEntries")
                    .document(book.getDocumentId())
                    .update("listId", com.google.firebase.firestore.FieldValue.delete())
                    .addOnSuccessListener(aVoid->{
                        //remove the card from the screen
                        int currentPosition = bookList.indexOf(book);
                        if(currentPosition != -1) {
                            bookList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                        }
                        Toast.makeText(holder.itemView.getContext(),
                                "Remove from to be read",Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Could not remove book", Toast.LENGTH_SHORT).show()
                    );
        });


        //set up the three dot menu
        holder.bookMenu.setOnClickListener(v -> {

            //Create a popup menu attached to the tree dot button
            PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), holder.bookMenu);

            //add the delete book option
            popupMenu.getMenu().add("Delete book");

            //Handle the menu option when selected
            popupMenu.setOnMenuItemClickListener(item -> {
                //Ask the user to confirm deleting
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Delete book")
                        .setMessage("Are you sure you want to delete this?")
                        .setPositiveButton("Delete", (dialog, which)->{
                            FirebaseFirestore.getInstance()
                                    .collection("readingEntries")
                                    .document(book.getDocumentId())
                                    .delete()
                                    .addOnSuccessListener(aVoid ->{

                                       //remove the book from the displayed list
                                        int currentPosition = bookList.indexOf(book);
                                        if(currentPosition != -1){
                                            bookList.remove(currentPosition);
                                            notifyItemRemoved(currentPosition);
                                        }
                                        Toast.makeText(holder.itemView.getContext(), "Book deleted",Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e->{
                                       //show an error if deletion fails
                                       Toast.makeText(holder.itemView.getContext(), "Could not delete book:" + e.getMessage(),Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
            //display the popup menu
            popupMenu.show();

        });
    }
    // how many books are in the to be read list
    @Override
    public int getItemCount(){
        return bookList.size();
    }
}



