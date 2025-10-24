package org.okane.voyagemapper;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private androidx.recyclerview.widget.RecyclerView resultsRecycler;
    private PlaceResultAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.useCurrentLocationBtn).setOnClickListener(v -> {
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("mode", "MY_LOCATION");
            startActivity(i);
        });

        searchEditText = findViewById(R.id.searchEditText);
        resultsRecycler = findViewById(R.id.resultsRecycler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceResultAdapter(item -> {
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("mode", "CENTER_AT");
            i.putExtra("lat", item.lat);
            i.putExtra("lon", item.lon);
            i.putExtra("title", item.title);
            startActivity(i);
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

    private void doSearch() {
        String q = String.valueOf(searchEditText.getText()).trim();
        if (q.isEmpty()) {
            Toast.makeText(this, "Enter a place to search", Toast.LENGTH_SHORT).show();
            return;
        }
        geocodePlaces(this, q, results -> {
            if (results.isEmpty()) {
                Toast.makeText(this, "No results", Toast.LENGTH_SHORT).show();
            }
            adapter.submit(results);
        });
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
                            if (label == null) label = "Unknown";
                            out.add(new PlaceResult(label, a.getLatitude(), a.getLongitude()));
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