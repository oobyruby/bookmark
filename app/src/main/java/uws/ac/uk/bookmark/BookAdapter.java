package uws.ac.uk.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private final ArrayList<Book> books;

    public BookAdapter(ArrayList<Book> books) {
        this.books = books;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);

        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BookViewHolder holder,
            int position) {

        Book book = books.get(position);

        // display book titles in lowercase
        String title = book.getTitle();

        holder.bookTitle.setText(
                title == null
                        ? ""
                        : title.toLowerCase(Locale.ROOT)
        );

        // load book cover
        String coverUrl = book.getCoverUrl();

        if (coverUrl != null && !coverUrl.isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(holder.bookCover);

        } else {

            Glide.with(holder.itemView.getContext())
                    .clear(holder.bookCover);

            holder.bookCover.setImageResource(
                    R.drawable.genre_tile_background
            );
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;
        TextView bookTitle;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);

            bookCover = itemView.findViewById(R.id.bookCover);
            bookTitle = itemView.findViewById(R.id.bookTitle);
        }
    }
}
