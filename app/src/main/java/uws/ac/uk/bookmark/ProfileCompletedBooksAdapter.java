
package uws.ac.uk.bookmark;

import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProfileCompletedBooksAdapter extends RecyclerView.Adapter<ProfileCompletedBooksAdapter.BookCoverViewHolder> {

    private final List<CurrentBook> bookList;

    // constructor receives the list of completed books
    public ProfileCompletedBooksAdapter(List<CurrentBook> bookList) {
        this.bookList = bookList;
    }

    // ViewHolder stores the ImageView used to display one book cover
    public static class BookCoverViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;

        public BookCoverViewHolder(@NonNull ImageView imageView) {
            super(imageView);

            // Connect the ImageView to this ViewHolder
            bookCover = imageView;
        }
    }

    // Creates the ImageView for each book cover
    @NonNull
    @Override
    public BookCoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Create an ImageView programmatically (no extra XML file needed)
        ImageView imageView = new ImageView(parent.getContext());

        // Convert dimensions from dp to pixels
        float density = parent.getResources().getDisplayMetrics().density;

        // Set the width and height of each book cover
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                (int) (85 * density),
                (int) (115 * density)
        );

        // Add a small space between book covers
        params.setMarginEnd((int) (8 * density));

        imageView.setLayoutParams(params);

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        return new BookCoverViewHolder(imageView);
    }

    // Displays the correct book cover at each RecyclerView position
    @Override
    public void onBindViewHolder(@NonNull BookCoverViewHolder holder, int position) {

        // Get the book at the current position in the list
        CurrentBook book = bookList.get(position);

        // Use Glide to load the book cover from its image URL
        Glide.with(holder.bookCover.getContext())
                .load(book.getCoverUrl())

                // show a placeholder while the cover image loads
                .placeholder(R.drawable.genre_tile_background)

                // display the loaded image in the ImageView
                .into(holder.bookCover);
    }

    // Tells the RecyclerView how many completed books are in the list
    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
