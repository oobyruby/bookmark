package uws.ac.uk.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class ProfileCompletedBooksAdapter extends RecyclerView.Adapter<ProfileCompletedBooksAdapter.BookCoverViewHolder> {

    private final List<CurrentBook> bookList;

    // constructor receives the list of completed books
    public ProfileCompletedBooksAdapter(List<CurrentBook> bookList) {
        this.bookList = bookList;
    }

    // ViewHolder stores the ImageView used to display one book cover
    public static class BookCoverViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;
        TextView bookTitle;

        public BookCoverViewHolder(@NonNull View itemView) {
            super(itemView);

            // Connect the ImageView to this ViewHolder
            bookCover = itemView.findViewById(R.id.latestReadCover);
            bookTitle = itemView.findViewById(R.id.latestReadTitle);
        }
    }

    // Creates the ImageView for each book cover
    @NonNull
    @Override
    public BookCoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Create the book card using the XML layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_latest_read, parent, false);

        return new BookCoverViewHolder(view);
    }

    // Displays the correct book cover at each RecyclerView position
    @Override
    public void onBindViewHolder(@NonNull BookCoverViewHolder holder, int position) {

        // Get the book at the current position in the list
        CurrentBook book = bookList.get(position);

        // Display the book title underneath the cover
        String title = book.getTitle();

        holder.bookTitle.setText(
                title == null ? "" : title.toLowerCase(Locale.ROOT)
        );

        // Use Glide to load the book cover from its image URL
        Glide.with(holder.bookCover.getContext())
                .load(book.getCoverUrl())

                // show a placeholder while the cover image loads
                .placeholder(R.drawable.genre_tile_background)
                .error(R.drawable.genre_tile_background)

                // display the loaded image in the ImageView
                .into(holder.bookCover);
    }

    // Tells the RecyclerView how many completed books are in the list
    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
