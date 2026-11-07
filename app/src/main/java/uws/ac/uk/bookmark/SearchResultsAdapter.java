
package uws.ac.uk.bookmark;

import android.content.Intent;
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

        // Open Book View when a book is selected
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    v.getContext(),
                    BookViewActivity.class
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

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {

        ImageView bookCover;
        TextView bookTitle;
        TextView bookAuthors;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);

            bookCover = itemView.findViewById(R.id.resultBookCover);
            bookTitle = itemView.findViewById(R.id.resultBookTitle);
            bookAuthors = itemView.findViewById(R.id.resultBookAuthors);
        }
    }
}
