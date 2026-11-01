package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        // return to homepage
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // open explore
        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ExploreActivity.class);
            startActivity(intent);
        });

        // map will be connected when its screen exists
        navMap.setOnClickListener(v -> {
        });

        // profile is already open
        navProfile.setOnClickListener(v -> {
        });
    }
}
