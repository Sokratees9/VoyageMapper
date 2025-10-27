package org.okane.voyagemapper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        if (item.getKind() == PlaceItem.Kind.ARTICLE) {
            View v = getLayoutInflater().inflate(R.layout.bottom_sheet_article, null);
            dialog.setContentView(v);

            TextView title = v.findViewById(R.id.title);
            TextView snippet = v.findViewById(R.id.snippet);
            ImageView thumb = v.findViewById(R.id.thumb);
            MaterialButton openBtn = v.findViewById(R.id.openButton);
            MaterialButton mapSightsBtn = v.findViewById(R.id.mapSightsButton);

            title.setText(item.getTitle());
            setThumbOrHide(thumb, item.getThumbUrl());
            snippet.setText(empty(item.getSnippet()) ? getString(R.string.preview) : item.getSnippet());

            openBtn.setOnClickListener(b -> {
                String url = "https://en.wikivoyage.org/?curid=" + item.getPageId();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
            mapSightsBtn.setOnClickListener(b -> {
                dialog.dismiss();
                mapSightsForPage(item.getPageId(), item.getTitle());
            });

        } else {
            View v = getLayoutInflater().inflate(R.layout.bottom_sheet_sight, null);
            dialog.setContentView(v);

            TextView title = v.findViewById(R.id.title);
            TextView content = v.findViewById(R.id.content);
            ImageView thumb = v.findViewById(R.id.thumb);

            title.setText(item.getTitle());
            content.setText(empty(item.getSnippet()) ? getString(R.string.preview) : item.getSnippet());
            setThumbOrHide(thumb, item.getThumbUrl());

            bindRow(v, R.id.phoneRow, R.id.phone, item.getPhone(), val ->
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + val))));

            bindRow(v, R.id.websiteRow, R.id.website, item.getUrl(), val ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(val))));

            bindRow(v, R.id.addressRow, R.id.address, item.getAddress(), val -> {
                Uri gmm = Uri.parse("geo:" + item.getPosition().latitude + "," + item.getPosition().longitude +
                        "?q=" + Uri.encode(val));
                startActivity(new Intent(Intent.ACTION_VIEW, gmm));
            });

            bindRow(v, R.id.hoursRow, R.id.hours, item.getHours(), null);
            bindRow(v, R.id.priceRow, R.id.price, item.getPrice(), null);

            // If you extract a Wikipedia link out of the listing:
            bindRow(v, R.id.wikiRow, R.id.wiki, item.getWikipediaUrl(), val ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(val))));
        }

        dialog.show();
    }

    private void bindRow(View root, int rowId, int textId, @Nullable String value,
            @Nullable java.util.function.Consumer<String> onClick) {
        View row = root.findViewById(rowId);
        TextView tv = root.findViewById(textId);
        if (empty(value)) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            tv.setText(value);
            if (onClick != null) row.setOnClickListener(v -> onClick.accept(value));
        }
    }

    private void setThumbOrHide(ImageView iv, @Nullable String url) {
        if (!empty(url)) {
            iv.setVisibility(View.VISIBLE);
            Glide.with(this).load(url).into(iv);
        } else {
            iv.setVisibility(View.GONE);
        }
    }

    private boolean empty(@Nullable String s) { return s == null || s.trim().isEmpty(); }


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
                    Toast.makeText(
                            MapActivity.this,
                            "No sights found in " + pageTitle + ", it's either quiet here or there are districts to explore.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add markers (reuse your PlaceItem so they render with labeled pins)
                List<PlaceItem> pins = new ArrayList<>();
                for (SeeListing s : listings) {
                    String snippet = (s.content != null ? s.content + " " : "")
                            + (s.phone != null ? "☎ " + s.phone + "  " : "")
                            + (s.url != null ? s.url : "");
                    pins.add(new PlaceItem(s.phone, s.url, s.address, s.hours, s.price, s.wikipediaUrl,
                            s.lat, s.lon, s.name, s.content, /*thumb*/ null, /*pageId*/ pageId, PlaceItem.Kind.SIGHT));
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
