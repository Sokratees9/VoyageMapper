package org.okane.voyagemapper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private androidx.recyclerview.widget.RecyclerView resultsRecycler;
    private PlaceResultAdapter adapter;
    private PlacesClient placesClient;
    private View searchPrompt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root); // your top-level container in activity_main.xml
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        String apiKey = readMapsKeyFromManifest();
        Log.d("VoyageMapper", "Maps/Places key prefix: " + (apiKey != null && apiKey.length() >= 8 ? apiKey.substring(0,8) : "NULL"));
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = com.google.android.libraries.places.api.Places.createClient(this);

        findViewById(R.id.useCurrentLocationBtn).setOnClickListener(v -> {
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("mode", "MY_LOCATION");
            startActivity(i);
        });

        searchEditText = findViewById(R.id.searchEditText);
        searchPrompt = findViewById(R.id.searchPrompt);
        resultsRecycler = findViewById(R.id.resultsRecycler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlaceResultAdapter(item -> {
            if (item.placeId == null) {
                return;
            }

            List<com.google.android.libraries.places.api.model.Place.Field> fields = Arrays.asList(
                    com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,
                    com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
                    com.google.android.libraries.places.api.model.Place.Field.WEBSITE_URI,
                    com.google.android.libraries.places.api.model.Place.Field.PHONE_NUMBER
            );

            com.google.android.libraries.places.api.net.FetchPlaceRequest req =
                    com.google.android.libraries.places.api.net.FetchPlaceRequest.newInstance(item.placeId, fields);

            placesClient.fetchPlace(req).addOnSuccessListener(response -> {
                com.google.android.libraries.places.api.model.Place place = response.getPlace();
                if (place.getLatLng() == null) return;

                double lat = place.getLatLng().latitude;
                double lon = place.getLatLng().longitude;

                // Launch your MapActivity centered here (your existing flow)
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                i.putExtra("mode", "CENTER_AT");
                i.putExtra("lat", lat);
                i.putExtra("lon", lon);
                i.putExtra("title", place.getName());
                startActivity(i);
            }).addOnFailureListener(err -> {
                // Show a toast or log
            });
        });
        resultsRecycler.setAdapter(adapter);

        findViewById(R.id.searchButton).setOnClickListener(v -> doSearch());
        // Also handle keyboard search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    private String readMapsKeyFromManifest() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle b = ai.metaData;
            return b.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) { return null; }
    }

    private void doSearch() {
        String q = String.valueOf(searchEditText.getText()).trim();
        if (q.isEmpty()) {
            Toast.makeText(this, "Enter a place to search", Toast.LENGTH_SHORT).show();
            return;
        }
//        geocodePlaces(this, q, results -> {
//            if (results.isEmpty()) {
//                Toast.makeText(this, "No results", Toast.LENGTH_SHORT).show();
//            }
//            adapter.submit(results);
//        });
        searchPlaces(q);
    }

    private void searchPlaces(String query) {
        // One token per “search session” helps billing & quality
        com.google.android.libraries.places.api.model.AutocompleteSessionToken token =
                com.google.android.libraries.places.api.model.AutocompleteSessionToken.newInstance();

        com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest req =
                com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        // Optional: bias to a region or to cities
                        // .setTypeFilter(com.google.android.libraries.places.api.model.TypeFilter.CITIES)
                        // .setCountries(Arrays.asList("GB","AU")) // if you want to limit
                        .setSessionToken(token)
                        .build();

        placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener(resp -> {
                    List<PlaceResult> results = new ArrayList<>();
                    for (com.google.android.libraries.places.api.model.AutocompletePrediction p : resp.getAutocompletePredictions()) {
                        // Show primary + secondary text in the list
                        String label = p.getPrimaryText(null) + (p.getSecondaryText(null).length() > 0
                                ? " — " + p.getSecondaryText(null) : "");
                        // We don’t have lat/lng yet; fetch on click. Store placeId.
                        PlaceResult pr = new PlaceResult(label, p.getPlaceId());
                        results.add(pr);
                    }
                    showResults(results, true);
                })
                .addOnFailureListener(e -> {
                    // Fallback: optionally call your old Geocoder here
                    showResults(Collections.emptyList(), false);
                });
    }

    private void showResults(List<PlaceResult> results, boolean success) {
        if (success) {
            searchPrompt.setAlpha(0f);
            searchPrompt.animate().alpha(1f).setDuration(300).start();
            resultsRecycler.setAlpha(0f);
            resultsRecycler.animate().alpha(1f).setDuration(300).start();
            searchPrompt.setVisibility(View.VISIBLE);
            resultsRecycler.setVisibility(View.VISIBLE);
        } else {
            searchPrompt.setVisibility(View.GONE);
            resultsRecycler.setVisibility(View.GONE);
        }
        adapter.submit(results);
    }

    // Simple background geocoding using the platform Geocoder
    private void geocodePlaces(Context ctx, String query, GeocodeCallback cb) {
        new Thread(() -> {
            List<PlaceResult> out = new ArrayList<>();
            try {
                Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
                List<Address> list = geocoder.getFromLocationName(query, 10);
                if (list != null) {
                    for (Address a : list) {
                        if (a.hasLatitude() && a.hasLongitude()) {
                            String label = a.getFeatureName();
                            if (label == null || label.isEmpty()) {
                                label = a.getLocality();
                            }
                            if (label == null || label.isEmpty()) {
                                label = a.getAdminArea();
                            }
                            if (label == null || label.isEmpty()) {
                                label = a.getCountryName();
                            }
                            if (label == null) {
                                label = "Unknown";
                            }
                            out.add(new PlaceResult(label, ""));
                        }
                    }
                }
            } catch (IOException ignored) { }
            List<PlaceResult> finalOut = out;
            runOnUiThread(() -> cb.onDone(finalOut));
        }).start();
    }

    interface GeocodeCallback { void onDone(List<PlaceResult> results); }
}