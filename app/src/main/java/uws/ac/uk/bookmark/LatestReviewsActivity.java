package uws.ac.uk.bookmark;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class LatestReviewsActivity extends AppCompatActivity {

    private LatestReviewsFeed latestReviewsFeed;

    private AmbientLightHelper ambientLightHelper;
    private boolean darkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_latest_reviews);

        getWindow().setStatusBarColor(
                Color.parseColor("#BCA9CC")
        );

        getWindow().setNavigationBarColor(
                Color.parseColor("#BCA9CC")
        );

        LinearLayout reviewsContainer =
                findViewById(R.id.allReviewsContainer);

        latestReviewsFeed = new LatestReviewsFeed(
                this,
                reviewsContainer
        );

        // return to the previous page
        findViewById(R.id.backButton).setOnClickListener(v ->
                finish());

        setupNavigation();
        setupAmbientLightSensor();
    }

    // connect the shared bottom navigation
    private void setupNavigation() {

        findViewById(R.id.navHome).setOnClickListener(v -> {

            Intent intent = new Intent(this, MainActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
        });

        findViewById(R.id.navExplore).setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        ExploreActivity.class
                )));

        findViewById(R.id.navMap).setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        NearbyLibrariesActivity.class
                )));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        ProfileActivity.class
                )));
    }

    // initialise the ambient light sensor
    private void setupAmbientLightSensor() {

        ambientLightHelper = new AmbientLightHelper(
                this,
                lux -> {

                    if (lux < 30 && !darkMode) {

                        applyBookTheme(true);

                    } else if (lux > 150 && darkMode) {

                        applyBookTheme(false);
                    }
                }
        );
    }

    // apply the light or dark theme to the reviews page
    private void applyBookTheme(boolean dark) {

        darkMode = dark;

        findViewById(R.id.latestReviewsRoot)
                .setBackgroundColor(Color.parseColor(
                        dark ? "#1E1A23" : "#FFFFFF"
                ));

        if (latestReviewsFeed != null) {
            latestReviewsFeed.setDarkMode(dark);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // retrieve all reviews without a limit
        if (latestReviewsFeed != null) {
            latestReviewsFeed.loadReviews(0);
        }

        if (ambientLightHelper != null) {
            ambientLightHelper.start();
        }
    }

    @Override
    protected void onPause() {

        if (latestReviewsFeed != null) {
            latestReviewsFeed.stopListening();
        }

        if (ambientLightHelper != null) {
            ambientLightHelper.stop();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (latestReviewsFeed != null) {
            latestReviewsFeed.shutdown();
        }

        super.onDestroy();
    }
}