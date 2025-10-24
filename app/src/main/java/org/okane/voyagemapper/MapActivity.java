package org.okane.voyagemapper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ClusterManager<PlaceItem> clusterManager;
    private final WikiRepository repo = new WikiRepository();
    private static final int REQ_LOC = 42;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setupClustering();

        // Default: London fallback
        LatLng fallback = new LatLng(51.5074, -0.1278);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f));

        String mode = getIntent().getStringExtra("mode");
        if ("CENTER_AT".equals(mode)) {
            double lat = getIntent().getDoubleExtra("lat", fallback.latitude);
            double lon = getIntent().getDoubleExtra("lon", fallback.longitude);
            String title = getIntent().getStringExtra("title");
            LatLng target = new LatLng(lat, lon);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 13f));
            if (title != null) {
                map.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                        .position(target).title(title));
            }
            // 👉 Load articles within 20 km of the searched place
            loadNearbyFor(target.latitude, target.longitude);

        } else if ("MY_LOCATION".equals(mode)) {
            // 👉 Center on the user, then load articles within 20 km
            enableMyLocationAndCenterAndLoad();

        } else {
            // No mode: still load nearby to fallback (optional)
            loadNearbyFor(fallback.latitude, fallback.longitude);
        }

        // Optional: refresh when user moves the camera a lot (basic throttle).
        map.setOnCameraIdleListener(() -> {
            if (clusterManager != null) clusterManager.onCameraIdle();
            LatLng center = map.getCameraPosition().target;
            // Uncomment if you want dynamic reloads on pan/zoom:
            // loadNearbyFor(center.latitude, center.longitude);
        });
    }

    private void setupClustering() {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setRenderer(new PlaceClusterRenderer(this, map, clusterManager));
        map.setOnMarkerClickListener(clusterManager);
        map.setOnCameraIdleListener(clusterManager);

        clusterManager.setOnClusterItemClickListener(item -> {
            map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngZoom(item.getPosition(), Math.max(map.getCameraPosition().zoom, 14f)));
            showPlaceSheet(item);
            return true; // consume the click
        });
    }

    private void showPlaceSheet(PlaceItem item) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View v = getLayoutInflater().inflate(R.layout.bottom_sheet_place, null);
        dialog.setContentView(v);

        android.widget.TextView title   = v.findViewById(R.id.title);
        android.widget.TextView snippet = v.findViewById(R.id.snippet);
        android.widget.TextView details = v.findViewById(R.id.details);
        android.widget.ImageView thumb  = v.findViewById(R.id.thumb);
        android.view.View buttonRow     = v.findViewById(R.id.buttonRow);
        com.google.android.material.button.MaterialButton openBtn = v.findViewById(R.id.openButton);
        com.google.android.material.button.MaterialButton mapSightsBtn = v.findViewById(R.id.mapSightsButton);

        title.setText(item.getTitle());

        // Thumbnail
        if (item.getThumbUrl() != null && !item.getThumbUrl().isEmpty()) {
            thumb.setVisibility(android.view.View.VISIBLE);
            Glide.with(this).load(item.getThumbUrl()).into(thumb);
        } else {
            thumb.setVisibility(android.view.View.GONE);
        }

        if (item.getKind() == PlaceItem.Kind.ARTICLE) {
            // 🟩 Black pin: show snippet + two buttons
            snippet.setVisibility(android.view.View.VISIBLE);
            snippet.setText(item.getSnippet() == null || item.getSnippet().isEmpty()
                    ? "Open the article for details."
                    : item.getSnippet());

            buttonRow.setVisibility(android.view.View.VISIBLE);
            details.setVisibility(android.view.View.GONE);

            openBtn.setOnClickListener(b -> {
                String url = "https://en.wikivoyage.org/?curid=" + item.getPageId();
                startActivity(new android.content.Intent(
                        android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
            });

            mapSightsBtn.setOnClickListener(b -> {
                dialog.dismiss();
                mapSightsForPage(item.getPageId(), item.getTitle());
            });

        } else {
            // 🟩 Green pin: hide snippet + hide buttons; show details instead
            snippet.setVisibility(android.view.View.GONE);
            buttonRow.setVisibility(android.view.View.GONE);
            details.setVisibility(android.view.View.VISIBLE);

            StringBuilder info = new StringBuilder();
            if (item.getSnippet() != null && !item.getSnippet().isEmpty()) {
                info.append(item.getSnippet()).append("\n");
            }
            info.append(String.format("Lat: %.5f  Lon: %.5f",
                    item.getPosition().latitude, item.getPosition().longitude));

            details.setText(info.toString());
        }

        dialog.show();
    }

    // ---- WIKIVOYAGE FETCH + CLUSTERING ----
    private void loadNearbyFor(double lat, double lon) {
        repo.loadNearby20km(lat, lon, new Callback<WikiResponse>() {
            @Override public void onResponse(Call<WikiResponse> call, Response<WikiResponse> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().query == null)
                    return;
                Map<String, WikiResponse.Page> pages = res.body().query.pages;

                List<PlaceItem> items = new ArrayList<>();
                for (WikiResponse.Page p : pages.values()) {
                    if (p.coordinates != null && !p.coordinates.isEmpty()) {
                        WikiResponse.Coordinate c = p.coordinates.get(0);
                        String snippet = p.extract != null ? p.extract : "";
                        String thumb = p.thumbnail != null ? p.thumbnail.source : null;
                        items.add(new PlaceItem(c.lat, c.lon, p.title, snippet, thumb, p.pageid, PlaceItem.Kind.ARTICLE));
                    }
                }

                clusterManager.clearItems();
                clusterManager.addItems(items);
                clusterManager.cluster();
            }
            @Override public void onFailure(Call<WikiResponse> call, Throwable t) {
                // You could Toast/log here
            }
        });
    }

    // ---- LOCATION PERMISSION + CENTER ----
    private void enableMyLocationAndCenterAndLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;
        }
        map.setMyLocationEnabled(true);
        FusedLocationProviderClient fused =
                LocationServices.getFusedLocationProviderClient(this);

        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
                loadNearbyFor(me.latitude, me.longitude); // 👉 fetch after centering
            }
        });
    }

    private void mapSightsForPage(long pageId, String pageTitle) {
        // Optional: show a lightweight progress indicator (Snackbar/Toast)
        // Toast.makeText(this, "Loading sights from " + pageTitle, Toast.LENGTH_SHORT).show();

        new WikiRepository().fetchPageWikitext(pageId, new retrofit2.Callback<PageContentResponse>() {
            @Override public void onResponse(retrofit2.Call<PageContentResponse> call,
                    retrofit2.Response<PageContentResponse> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().query == null
                        || res.body().query.pages == null || res.body().query.pages.isEmpty()) {
                    return;
                }
                String wikitext = res.body().query.pages.get(0).revisions.get(0).slots.main.content;

                List<SeeListing> listings = WikitextSeeParser.parse(wikitext);
                if (listings.isEmpty()) {
                    // Toast.makeText(MapActivity.this, "No sights with coordinates found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add markers (reuse your PlaceItem so they render with labeled pins)
                List<PlaceItem> pins = new ArrayList<>();
                for (SeeListing s : listings) {
                    String snippet = s.content + (s.phone != null ? " ☎ " + s.phone + "  " : "")
                            + (s.url != null ? s.url : "");
                    pins.add(new PlaceItem(
                            s.lat, s.lon, s.name, snippet, /*thumb*/ null, /*pageId*/ pageId, PlaceItem.Kind.SIGHT));
                }

                // Drop them on the map (+ keep existing items)
                clusterManager.addItems(pins);
                clusterManager.cluster();

                // Zoom to bounds of the new sights (optional, but nice)
                com.google.android.gms.maps.model.LatLngBounds.Builder b =
                        new com.google.android.gms.maps.model.LatLngBounds.Builder();
                for (PlaceItem p : pins) b.include(p.getPosition());
                try {
                    map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(b.build(), 80));
                } catch (Exception ignored) { /* if only one point, bounds fail; it's fine */ }
            }

            @Override public void onFailure(retrofit2.Call<PageContentResponse> call, Throwable t) {
                // Toast.makeText(MapActivity.this, "Failed to load sights.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onRequestPermissionsResult(int r, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQ_LOC && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndCenterAndLoad();
        }
    }
}
