package uws.ac.uk.bookmark;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class ExploreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navExplore = findViewById(R.id.navExplore);
        ImageButton navMap = findViewById(R.id.navMap);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ExploreActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        navExplore.setOnClickListener(v -> {
            // explore is already open
        });

        navMap.setOnClickListener(v -> {
            // connect when the map screen exists
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ExploreActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }
}
