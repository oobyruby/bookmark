package uws.ac.uk.bookmark;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

// audio player imports
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Locale;

public class BookViewActivity extends AppCompatActivity {

    // monitor ambient lighting on the book details screen
    private AmbientLightHelper ambientLightHelper;
    private boolean darkThemeEnabled = false;

    // locally stored narrated synopsis player
    private MediaPlayer audioPlayer;
    private SeekBar audioSeekBar;
    private TextView audioPlayPauseButton;
    private TextView audioCurrentTime;
    private TextView audioTotalTime;

    // update the playback progress while the audio is playing
    private final Handler audioHandler = new Handler(Looper.getMainLooper());

    private final Runnable audioProgressUpdater = new Runnable() {
        @Override
        public void run() {
            if (audioPlayer != null) {
                try {
                    int position = audioPlayer.getCurrentPosition();

                    if (audioSeekBar != null) {
                        audioSeekBar.setProgress(position);
                    }

                    if (audioCurrentTime != null) {
                        audioCurrentTime.setText(formatAudioTime(position));
                    }

                    if (audioPlayer.isPlaying()) {
                        audioHandler.postDelayed(this, 250);
                    }
                } catch (IllegalStateException e) {
                    android.util.Log.e(
                            "BookViewActivity",
                            "Could not update audio progress",
                            e
                    );
                }
            }
        }
    };

    // Information passed from the Search Results screen
    public static final String EXTRA_TITLE = "book_title";
    public static final String EXTRA_AUTHORS = "book_authors";
    public static final String EXTRA_COVER_URL = "book_cover_url";
    public static final String EXTRA_DESCRIPTION = "book_description";
    public static final String EXTRA_ID = "book_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_view);

        // initialise the ambient light sensor
        ambientLightHelper = new AmbientLightHelper(this, lux -> {
            Log.d("AmbientLight", "Light level: " + lux + " lux");

            if (lux < 30 && !darkThemeEnabled) {
                applyBookTheme(true);
            } else if (lux > 150 && darkThemeEnabled) {
                applyBookTheme(false);
            }
        });

        if (!ambientLightHelper.isSensorAvailable()) {
            Log.d("AmbientLight", "Light sensor not available on this device");
        }

        // Connect Java to the XML layout
        ImageView bookCover = findViewById(R.id.bookCover);
        TextView bookTitle = findViewById(R.id.bookTitle);
        TextView bookAuthors = findViewById(R.id.bookAuthors);
        TextView bookDescription = findViewById(R.id.bookDescription);
        TextView readMoreButton = findViewById(R.id.readMoreButton);
        TextView bookRating = findViewById(R.id.bookRating);
        TextView backButton = findViewById(R.id.backButton);

        // Connect the reading list and review elements
        TextView addToShelfButton = findViewById(R.id.addToShelfButton);
        TextView currentlyReadingButton = findViewById(R.id.currentlyReadingButton);
        RatingBar bookReviewRating = findViewById(R.id.bookReviewRating);
        TextView writeReviewButton = findViewById(R.id.writeReviewButton);

        // Get the selected book's information
        Intent intent = getIntent();

        String title = intent.getStringExtra(EXTRA_TITLE);
        String authors = intent.getStringExtra(EXTRA_AUTHORS);
        String coverUrl = intent.getStringExtra(EXTRA_COVER_URL);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);

        // check whether the selected book has a custom stephen king synopsis
        String customSynopsis = StephenKingSynopses.getSynopsis(title, authors);

// override the api description for the five selected books
        if (customSynopsis != null) {
            description = customSynopsis;
        }

        // Retrieve the selected book's ID
        String bookId = intent.getStringExtra(EXTRA_ID);

        Book selectedBook = new Book(
                bookId,
                title,
                authors,
                description,
                coverUrl
        );

        // Display the book title
        if (title != null && !title.isEmpty()) {
            bookTitle.setText(title.toLowerCase(Locale.ROOT));
        } else {
            bookTitle.setText(R.string.book_title_not_available);
        }

        // Display the author
        if (authors != null && !authors.isEmpty()) {
            bookAuthors.setText(authors.toLowerCase(Locale.ROOT));
        } else {
            bookAuthors.setText(R.string.author_not_available);
        }

        // Display the book cover using Glide
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .centerInside()
                    .placeholder(R.drawable.genre_tile_background)
                    .error(R.drawable.genre_tile_background)
                    .into(bookCover);
        } else {
            bookCover.setImageResource(
                    R.drawable.genre_tile_background
            );
        }


        // Display the book description
        if (description != null && !description.isEmpty()) {
            bookDescription.setText(
                    Html.fromHtml(
                            description,
                            Html.FROM_HTML_MODE_COMPACT
                    )
            );
        } else {
            bookDescription.setText(
                    R.string.book_description_not_available
            );
        }

        // expand or collapse longer descriptions
        bookDescription.setMaxLines(6);
        bookDescription.setEllipsize(TextUtils.TruncateAt.END);

        bookDescription.post(() -> {
            if (bookDescription.getLayout() == null) {
                return;
            }

            int lastVisibleLine = bookDescription.getLineCount() - 1;

            boolean isTruncated = lastVisibleLine >= 0
                    && bookDescription.getLayout()
                    .getEllipsisCount(lastVisibleLine) > 0;

            readMoreButton.setVisibility(
                    isTruncated ? View.VISIBLE : View.GONE
            );
        });

        readMoreButton.setOnClickListener(v -> {
            boolean isExpanded =
                    "read less ‹".contentEquals(readMoreButton.getText());

            if (isExpanded) {
                bookDescription.setMaxLines(6);
                bookDescription.setEllipsize(TextUtils.TruncateAt.END);
                readMoreButton.setText("read more ›");
            } else {
                bookDescription.setMaxLines(Integer.MAX_VALUE);
                bookDescription.setEllipsize(null);
                readMoreButton.setText("read less ‹");
            }
        });

        // return to the previous screen
        backButton.setOnClickListener(v -> {
            finish();
        });


        // Add the selected book to the to be read list
        addToShelfButton.setOnClickListener(v ->
                addBookToShelf(
                        selectedBook,
                        "to be read"
                ));

        // Add the selected book to the currently reading list
        currentlyReadingButton.setOnClickListener(v ->
                addBookToShelf(
                        selectedBook,
                        "currently reading"
                ));

        // Open the review dialog when a star rating is selected
        bookReviewRating.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    if (fromUser && rating > 0) {
                        SearchResultsAdapter.showReviewDialog(
                                ratingBar,
                                selectedBook,
                                rating
                        );

                        ratingBar.setRating(0);
                    }
                });

        // Open the review dialog when the write review button is selected
        writeReviewButton.setOnClickListener(v ->
                SearchResultsAdapter.showReviewDialog(
                        v,
                        selectedBook,
                        bookReviewRating.getRating()
                ));

        // retrieve the book rating from google books
        loadBookRating(title, authors, bookRating);

        // set up the narrated synopsis player for the five selected books
        setupAudioPlayer(title, authors);

        // Bottom navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // Home navigation
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(
                    BookViewActivity.this,
                    MainActivity.class
            );
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        });

        // Explore navigation
        navExplore.setOnClickListener(v -> {
            Intent exploreIntent = new Intent(
                    BookViewActivity.this,
                    ExploreActivity.class
            );
            exploreIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(exploreIntent);
        });

        // Profile navigation
        navProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(
                    BookViewActivity.this,
                    ProfileActivity.class
            );
            startActivity(profileIntent);
        });
    }


    // Placeholder for connecting reading lists to Firebase :)
    private void addBookToShelf(Book book, String readingList) {

        Toast.makeText(
                BookViewActivity.this,
                readingList + " not connected yet",
                Toast.LENGTH_SHORT
        ).show();
    }


    // set up local mp3 playback for the five selected stephen king books
    private void setupAudioPlayer(String title, String authors) {

        LinearLayout audioContainer =
                findViewById(R.id.audioPlayerContainer);

        audioSeekBar = findViewById(R.id.audioSeekBar);
        audioPlayPauseButton = findViewById(R.id.audioPlayPauseButton);
        audioCurrentTime = findViewById(R.id.audioCurrentTime);
        audioTotalTime = findViewById(R.id.audioTotalTime);

        // connect the custom audio playback controls
        TextView rewindButton = findViewById(R.id.audioRewindButton);
        TextView forwardButton = findViewById(R.id.audioForwardButton);

        // check whether the book has one of the five custom synopses
        if (StephenKingSynopses.getSynopsis(title, authors) == null) {
            audioContainer.setVisibility(View.GONE);
            return;
        }

        // match the selected book to its locally stored mp3 file
        int audioResource = getStephenKingAudio(title);

        if (audioResource == 0) {
            audioContainer.setVisibility(View.GONE);
            return;
        }

        try {
            // create the media player using the selected audio file
            audioPlayer = MediaPlayer.create(this, audioResource);

            if (audioPlayer == null) {
                audioContainer.setVisibility(View.GONE);
                return;
            }

            audioContainer.setVisibility(View.VISIBLE);

            // display the total audio duration
            int duration = audioPlayer.getDuration();
            audioSeekBar.setMax(duration);
            audioTotalTime.setText(formatAudioTime(duration));
            audioCurrentTime.setText("0:00");

            // play or pause the narrated synopsis
            audioPlayPauseButton.setOnClickListener(v -> {

                if (audioPlayer == null) {
                    return;
                }

                if (audioPlayer.isPlaying()) {
                    audioPlayer.pause();
                    audioPlayPauseButton.setText("play");
                    audioHandler.removeCallbacks(audioProgressUpdater);

                } else {
                    audioPlayer.start();
                    audioPlayPauseButton.setText("pause");

                    audioHandler.removeCallbacks(audioProgressUpdater);
                    audioHandler.post(audioProgressUpdater);
                }
            });

            // rewind the audio by ten seconds
            rewindButton.setOnClickListener(v -> {

                if (audioPlayer != null) {
                    int position = Math.max(
                            0,
                            audioPlayer.getCurrentPosition() - 10000
                    );

                    seekAudioTo(position);
                }
            });

            // skip forward by ten seconds
            forwardButton.setOnClickListener(v -> {

                if (audioPlayer != null) {
                    int position = Math.min(
                            audioPlayer.getDuration(),
                            audioPlayer.getCurrentPosition() + 10000
                    );

                    seekAudioTo(position);
                }
            });

            // allow users to drag the playback progress bar
            audioSeekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onProgressChanged(
                                SeekBar seekBar,
                                int progress,
                                boolean fromUser) {

                            if (fromUser) {
                                audioCurrentTime.setText(
                                        formatAudioTime(progress)
                                );
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            seekAudioTo(seekBar.getProgress());
                        }
                    }
            );

            // reset the controls when the narration finishes
            audioPlayer.setOnCompletionListener(player -> {
                audioHandler.removeCallbacks(audioProgressUpdater);

                player.seekTo(0);
                audioSeekBar.setProgress(0);
                audioCurrentTime.setText("0:00");
                audioPlayPauseButton.setText("play");
            });

        } catch (Exception e) {
            android.util.Log.e(
                    "BookViewActivity",
                    "Could not initialise audio player",
                    e
            );

            audioHandler.removeCallbacks(audioProgressUpdater);

            if (audioPlayer != null) {
                audioPlayer.release();
                audioPlayer = null;
            }

            audioContainer.setVisibility(View.GONE);
        }
    }

    // identify the correct local audio file from the book title
    private int getStephenKingAudio(String title) {

        if (title == null) {
            return 0;
        }

        switch (normalise(title)) {

            case "theshining":
                return R.raw.the_shining;

            case "it":
                return R.raw.it;

            case "carrie":
                return R.raw.carrie;

            case "misery":
                return R.raw.misery;

            case "petsematary":
                return R.raw.pet_sematary;

            default:
                return 0;
        }
    }

    // seek to a selected position within the audio
    private void seekAudioTo(int position) {

        if (audioPlayer == null) {
            return;
        }

        audioPlayer.seekTo(position);
        audioSeekBar.setProgress(position);
        audioCurrentTime.setText(formatAudioTime(position));
    }

    // convert playback milliseconds into minutes and seconds
    private String formatAudioTime(int milliseconds) {

        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(
                Locale.UK,
                "%d:%02d",
                minutes,
                seconds
        );
    }

    // retrieve ratings from the google books api
    private void loadBookRating(
            String title,
            String authors,
            TextView ratingView) {

        if (title == null || title.trim().isEmpty()) {
            ratingView.setText("not yet rated");
            return;
        }

        new Thread(() -> {

            HttpURLConnection connection = null;
            String ratingText = "not yet rated";

            try {

                // search for the selected book by title and author
                String query = "intitle:\"" + title + "\"";

                if (authors != null && !authors.trim().isEmpty()) {
                    String firstAuthor = authors.split(",")[0].trim();
                    query += " inauthor:\"" + firstAuthor + "\"";
                }

                String endpoint =
                        "https://www.googleapis.com/books/v1/volumes?q="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&maxResults=20";

                connection = (HttpURLConnection)
                        new URL(endpoint).openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                if (connection.getResponseCode() == 200) {

                    // read the api response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    // extract the book results
                    JSONObject json =
                            new JSONObject(response.toString());

                    JSONArray items = json.optJSONArray("items");

                    if (items != null) {

                        double bestRating = -1;
                        int bestCount = -1;

                        // find the matching edition with the most ratings
                        for (int i = 0; i < items.length(); i++) {

                            JSONObject volumeInfo = items
                                    .getJSONObject(i)
                                    .optJSONObject("volumeInfo");

                            if (volumeInfo == null
                                    || !volumeInfo.has("averageRating")) {
                                continue;
                            }

                            String resultTitle =
                                    volumeInfo.optString("title", "");

                            if (!normalise(resultTitle)
                                    .equals(normalise(title))) {
                                continue;
                            }

                            double rating = volumeInfo.optDouble(
                                    "averageRating", -1
                            );

                            int count = volumeInfo.optInt(
                                    "ratingsCount", 0
                            );

                            if (rating >= 0 && rating <= 5
                                    && count > bestCount) {
                                bestRating = rating;
                                bestCount = count;
                            }
                        }

                        // format the rating for display
                        if (bestRating >= 0) {
                            ratingText = String.format(
                                    Locale.UK,
                                    "★ %.1f / 5  (%d ratings)",
                                    bestRating,
                                    Math.max(bestCount, 0)
                            );
                        }
                    }
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "BookViewActivity",
                        "Could not load rating",
                        e
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // update the rating on the main thread
            String finalRatingText = ratingText;

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    ratingView.setText(finalRatingText);
                }
            });

        }).start();
    }

    // normalise book titles for matching api results
    private String normalise(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    // update the book details colours without recreating the activity
    private void applyBookTheme(boolean dark) {
        darkThemeEnabled = dark;

        int page = Color.parseColor(dark ? "#211D27" : "#FFFCFE");
        int panel = Color.parseColor(dark ? "#302938" : "#F4EDF8");
        int primary = Color.parseColor(dark ? "#F5F0F8" : "#29232C");
        int secondary = Color.parseColor(dark ? "#D9CCDF" : "#4B4350");
        int accent = Color.parseColor(dark ? "#C9B1DB" : "#826B94");

        findViewById(R.id.bookViewRoot).setBackgroundColor(page);
        findViewById(R.id.bookViewContent).setBackgroundColor(page);

        // tint the existing rounded panels rather than replacing their shapes
        findViewById(R.id.synopsisPanel).setBackgroundTintList(
                ColorStateList.valueOf(panel));
        findViewById(R.id.audioPlayerContainer).setBackgroundTintList(
                ColorStateList.valueOf(panel));

        ((TextView) findViewById(R.id.backButton)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookTitle)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookAuthors)).setTextColor(secondary);
        ((TextView) findViewById(R.id.bookRating)).setTextColor(accent);
        ((TextView) findViewById(R.id.synopsisHeading)).setTextColor(primary);
        ((TextView) findViewById(R.id.bookDescription)).setTextColor(secondary);
        ((TextView) findViewById(R.id.readMoreButton)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioHeading)).setTextColor(primary);
        ((TextView) findViewById(R.id.audioSubheading)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioCurrentTime)).setTextColor(secondary);
        ((TextView) findViewById(R.id.audioTotalTime)).setTextColor(secondary);
        ((TextView) findViewById(R.id.audioRewindButton)).setTextColor(accent);
        ((TextView) findViewById(R.id.audioForwardButton)).setTextColor(accent);

        Log.d("AmbientLight", dark ? "Dark theme enabled" : "Light theme enabled");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ambientLightHelper != null) {
            ambientLightHelper.start();
        }
    }

    @Override
    protected void onPause() {
        if (ambientLightHelper != null) {
            ambientLightHelper.stop();
        }
        super.onPause();
    }

    // pause the narration when the book screen is no longer visible
    @Override
    protected void onStop() {
        super.onStop();

        audioHandler.removeCallbacks(audioProgressUpdater);

        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();

            if (audioPlayPauseButton != null) {
                audioPlayPauseButton.setText("play");
            }
        }
    }

    // release the audio player when the activity is destroyed
    @Override
    protected void onDestroy() {

        audioHandler.removeCallbacks(audioProgressUpdater);

        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }

        super.onDestroy();
    }
}