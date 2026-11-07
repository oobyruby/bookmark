package uws.ac.uk.bookmark;

import android.graphics.Typeface;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NearbyLibrariesActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final String TAG = "NearbyLibraries";

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int SEARCH_RADIUS_METRES = 5000;
    private static final int MAX_RESULTS = 10;
    private static final long LOCATION_TIMEOUT_MS = 25000;

    // Layout components
    private Button findLibrariesButton;
    private ProgressBar librariesProgress;
    private TextView librariesStatus;
    private LinearLayout libraryResults;

    // Android location components
    private LocationManager locationManager;
    private CancellationSignal locationCancellation;
    private LocationListener legacyLocationListener;
    private Runnable locationTimeout;
    private boolean waitingForLocation;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // Google Maps and Google Places
    private GoogleMap googleMap;
    private PlacesClient placesClient;
    private LatLng searchCenter;

    // Results returned by Google Places
    private final ArrayList<NearbyPlaceResult> nearbyPlaces =
            new ArrayList<>();

    // Store information about a library or bookshop
    private static class NearbyPlaceResult {

        String name;
        String address;
        LatLng position;
        float distanceMetres;
        boolean isBookshop;

        NearbyPlaceResult(
                String name,
                String address,
                LatLng position,
                float distanceMetres,
                boolean isBookshop) {

            this.name = name;
            this.address = address;
            this.position = position;
            this.distanceMetres = distanceMetres;
            this.isBookshop = isBookshop;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nearby_libraries);

        // Connect Java to the XML layout
        findLibrariesButton =
                findViewById(R.id.findLibrariesButton);

        librariesProgress =
                findViewById(R.id.librariesProgress);

        librariesStatus =
                findViewById(R.id.librariesStatus);

        libraryResults =
                findViewById(R.id.libraryResults);

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        // Read the shared Google Maps key from Gradle resources
        String apiKey = getString(R.string.google_maps_key);

        // Keep technical configuration details hidden from users
        if (apiKey.trim().isEmpty()
                || apiKey.equals("DEFAULT_API_KEY")) {

            Log.e(TAG, "Google Maps configuration is unavailable");

            librariesStatus.setText(
                    R.string.nearby_libraries_error
            );

            findLibrariesButton.setEnabled(false);

        } else {

            // Initialise Google Places SDK (New)
            if (!Places.isInitialized()) {

                Places.initializeWithNewPlacesApiEnabled(
                        getApplicationContext(),
                        apiKey
                );
            }

            placesClient = Places.createClient(this);
        }

        // Connect Google Maps to this Activity
        SupportMapFragment mapFragment =
                (SupportMapFragment)
                        getSupportFragmentManager()
                                .findFragmentById(
                                        R.id.nearbyMapFragment
                                );

        if (mapFragment != null) {

            mapFragment.getMapAsync(this);
        }

        // Start a search when the button is pressed
        findLibrariesButton.setOnClickListener(v ->
                checkLocationPermission());

        // Preserve Bookmark bottom navigation
        setupNavigation();
    }

    // Called when the Google Map is ready
    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        // Enable the map's zoom controls
        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        // Open directions when marker information is selected
        googleMap.setOnInfoWindowClickListener(marker -> {

            Object tag = marker.getTag();

            if (tag instanceof LatLng) {

                openDirections((LatLng) tag);
            }
        });

        // Display any search results already available
        updateMap();
    }

    // Check precise or approximate location permission
    private boolean hasLocationPermission() {

        boolean fineAllowed =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseAllowed =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return fineAllowed || coarseAllowed;
    }

    // Request location permission when required
    private void checkLocationPermission() {

        if (hasLocationPermission()) {

            requestCurrentLocation();

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
        }
    }

    // Process the location permission result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (hasLocationPermission()) {

                requestCurrentLocation();

            } else {

                librariesStatus.setText(
                        R.string.nearby_libraries_permission
                );
            }
        }
    }

    // Find the current device or emulator location
    @SuppressLint("MissingPermission")
    private void requestCurrentLocation() {

        if (!hasLocationPermission() || locationManager == null) {

            showLocationError();
            return;
        }

        boolean fineAllowed =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        String provider;

        // Prefer GPS when precise location permission is granted
        if (fineAllowed &&
                locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER)) {

            provider = LocationManager.GPS_PROVIDER;

        } else if (locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)) {

            // Network location supports approximate permission
            provider = LocationManager.NETWORK_PROVIDER;

        } else {

            showLocationError();
            return;
        }

        // Show progress while finding the current location
        librariesStatus.setText(
                R.string.nearby_libraries_locating
        );

        setLoading(true);

        libraryResults.removeAllViews();
        nearbyPlaces.clear();

        // Stop any previous location request
        clearLocationRequest();

        waitingForLocation = true;

        // Prevent GPS from waiting indefinitely
        locationTimeout = () -> onLocationResult(null);

        mainHandler.postDelayed(
                locationTimeout,
                LOCATION_TIMEOUT_MS
        );

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                // Request current location on Android 11 or newer
                locationCancellation = new CancellationSignal();

                locationManager.getCurrentLocation(
                        provider,
                        locationCancellation,
                        getMainExecutor(),
                        this::onLocationResult
                );

            } else {

                // Support older Android versions
                legacyLocationListener = new LocationListener() {

                    @Override
                    public void onLocationChanged(
                            @NonNull Location location) {

                        onLocationResult(location);
                    }
                };

                locationManager.requestSingleUpdate(
                        provider,
                        legacyLocationListener,
                        Looper.getMainLooper()
                );
            }

        } catch (SecurityException | IllegalArgumentException e) {

            Log.e(TAG, "Location request failed", e);

            onLocationResult(null);
        }
    }

    // Process the location returned by Android
    private void onLocationResult(Location location) {

        if (!waitingForLocation) {
            return;
        }

        waitingForLocation = false;

        clearLocationRequest();

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (location == null) {

            showLocationError();

        } else {

            searchNearbyPlaces(location);
        }
    }

    // Cancel unfinished location requests
    private void clearLocationRequest() {

        if (locationTimeout != null) {

            mainHandler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }

        if (locationCancellation != null) {

            locationCancellation.cancel();
            locationCancellation = null;
        }

        if (legacyLocationListener != null
                && locationManager != null) {

            locationManager.removeUpdates(
                    legacyLocationListener
            );

            legacyLocationListener = null;
        }
    }

    // Display a location error to the user
    private void showLocationError() {

        setLoading(false);

        librariesStatus.setText(
                R.string.nearby_libraries_location_error
        );
    }

    // Search Google Places for libraries and bookshops
    private void searchNearbyPlaces(Location deviceLocation) {

        if (placesClient == null) {

            setLoading(false);

            // Display a normal user-facing error
            librariesStatus.setText(
                    R.string.nearby_libraries_error
            );

            return;
        }

        librariesStatus.setText(
                R.string.nearby_libraries_searching
        );

        // Use the device location as the search centre
        searchCenter = new LatLng(
                deviceLocation.getLatitude(),
                deviceLocation.getLongitude()
        );

        nearbyPlaces.clear();

        // Display the search area on the map
        updateMap();

        // Search within a 5 km circle
        CircularBounds bounds =
                CircularBounds.newInstance(
                        searchCenter,
                        SEARCH_RADIUS_METRES
                );

        // Request only the information needed for the screen
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.TYPES
        );

        // Search libraries and bookshops together
        // Return up to 10 results, ranked by distance
        SearchNearbyRequest request =
                SearchNearbyRequest.builder(
                                bounds,
                                placeFields
                        )
                        .setIncludedTypes(
                                Arrays.asList(
                                        "library",
                                        "book_store"
                                )
                        )
                        .setMaxResultCount(MAX_RESULTS)
                        .setRankPreference(
                                SearchNearbyRequest.RankPreference.DISTANCE
                        )
                        .build();

        // Perform an asynchronous search using Google Places
        placesClient.searchNearby(request)

                .addOnSuccessListener(response -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    nearbyPlaces.clear();

                    // Process each place returned by Google
                    for (Place place : response.getPlaces()) {

                        // Read the place coordinates
                        LatLng position = place.getLocation();

                        if (position == null) {
                            continue;
                        }

                        // Read the place name
                        String name = place.getDisplayName();

                        if (name == null || name.trim().isEmpty()) {

                            name = getString(
                                    R.string.nearby_library_default
                            );
                        }

                        // Read the place address
                        String address = place.getFormattedAddress();

                        if (address == null) {
                            address = "";
                        }

                        // Determine whether the result is a bookshop
                        List<String> placeTypes = place.getPlaceTypes();

                        boolean isBookshop =
                                placeTypes != null
                                        && placeTypes.contains(
                                        "book_store"
                                );

                        // Calculate distance from the user's location
                        float[] distance = new float[1];

                        Location.distanceBetween(
                                searchCenter.latitude,
                                searchCenter.longitude,
                                position.latitude,
                                position.longitude,
                                distance
                        );

                        // Only accept places within 5 km
                        if (distance[0] <= SEARCH_RADIUS_METRES) {

                            nearbyPlaces.add(
                                    new NearbyPlaceResult(
                                            name,
                                            address,
                                            position,
                                            distance[0],
                                            isBookshop
                                    )
                            );
                        }
                    }

                    // Sort places from nearest to furthest
                    Collections.sort(
                            nearbyPlaces,
                            (a, b) -> Float.compare(
                                    a.distanceMetres,
                                    b.distanceMetres
                            )
                    );

                    setLoading(false);

                    displayNearbyPlaces();
                    updateMap();
                })

                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    // Technical details remain in Logcat only
                    Log.e(
                            TAG,
                            "Google Places search failed",
                            e
                    );

                    setLoading(false);

                    // Show a simple error message to the user
                    librariesStatus.setText(
                            R.string.nearby_libraries_error
                    );
                });
    }

    // Update the interactive Google Map
    private void updateMap() {

        if (googleMap == null || searchCenter == null) {
            return;
        }

        // Remove previous markers and circles
        googleMap.clear();

        // Blue marker showing the user's current location
        googleMap.addMarker(
                new MarkerOptions()
                        .position(searchCenter)
                        .title(
                                getString(
                                        R.string.nearby_libraries_user_location
                                )
                        )
                        .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE
                                )
                        )
        );

        // Draw a purple circle showing the 5 km search radius
        googleMap.addCircle(
                new CircleOptions()
                        .center(searchCenter)
                        .radius(SEARCH_RADIUS_METRES)
                        .strokeColor(
                                Color.rgb(125, 82, 150)
                        )
                        .strokeWidth(3f)
                        .fillColor(
                                Color.argb(30, 125, 82, 150)
                        )
        );

        // Add a marker for each library or bookshop
        for (NearbyPlaceResult place : nearbyPlaces) {

            // Orange marker for bookshops
            // Purple marker for libraries
            float markerColour = place.isBookshop
                    ? BitmapDescriptorFactory.HUE_ORANGE
                    : BitmapDescriptorFactory.HUE_VIOLET;

            String category = getString(
                    place.isBookshop
                            ? R.string.nearby_place_bookshop
                            : R.string.nearby_place_library
            );

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(place.position)
                            .title(place.name)
                            .snippet(
                                    category + " · " +
                                            (place.address.isEmpty()
                                                    ? getString(
                                                    R.string.nearby_place_directions
                                            )
                                                    : place.address)
                            )
                            .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                            markerColour
                                    )
                            )
            );

            if (marker != null) {

                // Store the destination for walking directions
                marker.setTag(place.position);
            }
        }

        // Focus the camera on the search area
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        searchCenter,
                        12.5f
                )
        );
    }

    // Show nearby places beneath the map
    private void displayNearbyPlaces() {

        libraryResults.removeAllViews();

        int count = nearbyPlaces.size();

        if (count == 0) {

            librariesStatus.setText(
                    R.string.nearby_libraries_empty
            );

            return;
        }

        // Display singular or plural result count
        librariesStatus.setText(
                getResources().getQuantityString(
                        R.plurals.nearby_libraries_count,
                        count,
                        count
                )
        );

        for (NearbyPlaceResult place : nearbyPlaces) {

            // Create a card for each library or bookshop
            LinearLayout card = new LinearLayout(this);

            card.setOrientation(LinearLayout.VERTICAL);

            card.setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
            );

            card.setFocusable(true);

            // Apply Bookmark's existing purple styling
            GradientDrawable background =
                    new GradientDrawable();

            background.setColor(
                    Color.parseColor("#F5EFF7")
            );

            background.setCornerRadius(dp(10));

            card.setBackground(background);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(0, 0, 0, dp(10));

            // Display the name
            TextView nameText = new TextView(this);

            nameText.setText(place.name);
            nameText.setTextColor(
                    Color.parseColor("#333333")
            );

            nameText.setTextSize(16);
            nameText.setTypeface(null, Typeface.BOLD);

            card.addView(nameText);

            // Display the place category
            TextView categoryText = new TextView(this);

            categoryText.setText(
                    place.isBookshop
                            ? R.string.nearby_place_bookshop
                            : R.string.nearby_place_library
            );

            categoryText.setTextColor(
                    place.isBookshop
                            ? Color.rgb(180, 95, 20)
                            : Color.rgb(125, 82, 150)
            );

            categoryText.setTextSize(13);

            card.addView(categoryText);

            // Display the address when available
            if (!place.address.isEmpty()) {

                TextView addressText = new TextView(this);

                addressText.setText(place.address);

                addressText.setTextColor(
                        Color.parseColor("#555555")
                );

                addressText.setTextSize(13);

                card.addView(addressText);
            }

            // Display the approximate distance
            TextView distanceText = new TextView(this);

            distanceText.setText(
                    getString(
                            R.string.nearby_library_distance,
                            place.distanceMetres / 1000f
                    )
            );

            distanceText.setTextColor(
                    Color.parseColor("#555555")
            );

            distanceText.setTextSize(13);

            distanceText.setPadding(
                    0,
                    dp(6),
                    0,
                    0
            );

            card.addView(distanceText);

            // Open walking directions when a result is selected
            card.setOnClickListener(v ->
                    openDirections(place.position));

            libraryResults.addView(card, params);
        }
    }

    // Open walking directions using Google Maps
    private void openDirections(LatLng destination) {

        Uri uri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1"
                        + "&destination="
                        + destination.latitude
                        + "%2C"
                        + destination.longitude
                        + "&travelmode=walking"
        );

        try {

            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    )
            );

        } catch (Exception e) {

            Log.e(TAG, "Could not open directions", e);

            Toast.makeText(
                    this,
                    R.string.nearby_libraries_map_error,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Show or hide the loading indicator
    private void setLoading(boolean loading) {

        librariesProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        findLibrariesButton.setEnabled(!loading);
    }

    // Convert dp units into screen pixels
    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    // Preserve Bookmark's existing bottom navigation
    private void setupNavigation() {

        ImageButton navHome =
                findViewById(R.id.navHome);

        ImageButton navExplore =
                findViewById(R.id.navExplore);

        ImageButton navMap =
                findViewById(R.id.navMap);

        ImageButton navProfile =
                findViewById(R.id.navProfile);

        // Home navigation
        navHome.setOnClickListener(v -> {

            Intent intent = new Intent(
                    this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(intent);
        });

        // Explore navigation
        navExplore.setOnClickListener(v -> {

            Intent intent = new Intent(
                    this,
                    ExploreActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(intent);
        });

        // Maps navigation
        navMap.setOnClickListener(v -> {

            // Already on Nearby Libraries and Bookshops
        });

        // Profile navigation
        navProfile.setOnClickListener(v ->

                startActivity(
                        new Intent(
                                this,
                                ProfileActivity.class
                        )
                )
        );
    }

    @Override
    protected void onDestroy() {

        // Cancel unfinished location requests
        waitingForLocation = false;

        clearLocationRequest();

        super.onDestroy();
    }
}
