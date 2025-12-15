package org.okane.voyagemapper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.maps.android.clustering.ClusterManager;

import org.okane.voyagemapper.model.PlaceItem;
import org.okane.voyagemapper.model.SeeListing;
import org.okane.voyagemapper.service.NearbyCallback;
import org.okane.voyagemapper.service.NetworkErrorHandler;
import org.okane.voyagemapper.service.PageContentResponse;
import org.okane.voyagemapper.service.WikiRepository;
import org.okane.voyagemapper.service.WikiResponse;
import org.okane.voyagemapper.ui.PlaceClusterRenderer;
import org.okane.voyagemapper.util.MediaWikiUtils;
import org.okane.voyagemapper.util.NetworkUtils;
import org.okane.voyagemapper.util.TemplateMatcher;
import org.okane.voyagemapper.util.WikidataCoordsFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Callback;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private View loadingOverlay;
    private TextView loadingText;
    private ClusterManager<PlaceItem> clusterManager;
    private final WikiRepository repo = new WikiRepository();
    private static final int REQ_LOC = 42;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);
        setupClustering();

        SharedPreferences prefs = getSharedPreferences("voyage_prefs", MODE_PRIVATE);
        float lastLat = prefs.getFloat("last_lat", 0f);
        float lastLon = prefs.getFloat("last_lon", 0f);
        float zoom = prefs.getFloat("last_zoom", 0f);

        LatLng fallback;
        if (lastLat != 0f && lastLon != 0f) {
            fallback = new LatLng(lastLat, lastLon);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, zoom));
        } else {
            // Fallback: show user’s current location or a default region
            fallback = new LatLng(51.505, -0.09); // London, for example
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 11f));
        }

        String mode = getIntent().getStringExtra("mode");
        if ("CENTER_AT".equals(mode)) {
            showLoading(getString(R.string.loading_articles));
            double lat = getIntent().getDoubleExtra("lat", fallback.latitude);
            double lon = getIntent().getDoubleExtra("lon", fallback.longitude);
            LatLng target = new LatLng(lat, lon);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 11f));
            setLocationEnabled();
//            String title = getIntent().getStringExtra("title");
//            if (title != null) {
//                map.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
//                        .position(target).title(title));
//            }
            // 👉 Load articles within 20 km of the searched place
            loadNearbyFor(target.latitude, target.longitude);

        } else if ("MY_LOCATION".equals(mode)) {
            showLoading(getString(R.string.loading_articles_near_you));
            // 👉 Center on the user, then load articles within 20 km
            enableMyLocationAndCenterAndLoad();

        } else {
            showLoading(getString(R.string.loading_articles));
            // No mode: still load nearby to fallback (optional)
            loadNearbyFor(fallback.latitude, fallback.longitude);
        }

        FloatingActionButton refreshBtn = findViewById(R.id.refreshButton);
        refreshBtn.setOnClickListener(v -> {
            if (map != null) {
                showLoading(getString(R.string.refreshing_articles));
                v.animate().rotationBy(360f).setDuration(600).start();
                LatLng center = map.getCameraPosition().target;
                loadNearbyFor(center.latitude, center.longitude);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            LatLng target = map.getCameraPosition().target;
            float zoom = map.getCameraPosition().zoom;

            SharedPreferences prefs = getSharedPreferences("voyage_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putFloat("last_lat", (float) target.latitude)
                    .putFloat("last_lon", (float) target.longitude)
                    .putFloat("last_zoom", zoom)
                    .apply(); // Save asynchronously
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    private void setupClustering() {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setRenderer(new PlaceClusterRenderer(this, map, clusterManager));
        map.setOnMarkerClickListener(clusterManager);
        map.setOnInfoWindowClickListener(clusterManager);

        // FAB fade on move
        FloatingActionButton refreshBtn = findViewById(R.id.refreshButton);
        map.setOnCameraMoveStartedListener(reason ->
                refreshBtn.animate().alpha(0f).setDuration(150).start());

        // IMPORTANT: on idle, do BOTH: fade-in AND forward to cluster manager
        map.setOnCameraIdleListener(() -> {
            refreshBtn.animate().alpha(1f).setDuration(150).start();
            clusterManager.onCameraIdle();   // <- this keeps clustering working
        });

        clusterManager.setOnClusterItemClickListener(item -> {
            map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngZoom(item.getPosition(), Math.max(map.getCameraPosition().zoom, 13f)));
            showPlaceSheet(item);
            return true; // consume the click
        });
    }

    private void showLoading(String message) {
        if (loadingText != null) {
            loadingText.setText(message);
        }
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showPlaceSheet(PlaceItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        if (item.getKind() == PlaceItem.Kind.ARTICLE) {
            @SuppressLint("InflateParams")
            View v = getLayoutInflater().inflate(R.layout.bottom_sheet_article, null);
            dialog.setContentView(v);

            TextView title = v.findViewById(R.id.title);
            TextView snippet = v.findViewById(R.id.snippet);
            ImageView thumb = v.findViewById(R.id.thumb);
            thumb.setContentDescription("Image of " + item.getTitle());
            MaterialButton openBtn = v.findViewById(R.id.openButton);
            MaterialButton mapSightsBtn = v.findViewById(R.id.mapSightsButton);

            thumb.setOnClickListener(val -> {
                String thumbUrl = item.getThumbUrl();
                if (thumbUrl == null) return;

                // 1) remove '/thumb/' segment
                String noThumb = thumbUrl.replace("/thumb/", "/");

                // 2) strip the trailing '/<width>px-<filename>' segment entirely
                // e.g. '/250px-Colosseo_2020.jpg' at the end
                String fullUrl = noThumb.replaceAll("/\\d+px-[^/]+$", "");

                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
            });

            title.setText(item.getTitle());
            setThumbOrHide(thumb, item.getThumbUrl(), item.getTitle());
            snippet.setText(empty(item.getSnippet()) ? getString(R.string.no_actual_information) : item.getSnippet());

            openBtn.setOnClickListener(b -> {
                String url = "https://en.wikivoyage.org/?curid=" + item.getPageId();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
            mapSightsBtn.setOnClickListener(b -> {
                dialog.dismiss();
                mapSightsForPage(item);
            });

        } else {
            @SuppressLint("InflateParams")
            View v = getLayoutInflater().inflate(R.layout.bottom_sheet_sight, null);
            dialog.setContentView(v);

            TextView title = v.findViewById(R.id.title);
            TextView content = v.findViewById(R.id.content);
            ImageView thumb = v.findViewById(R.id.thumb);
            thumb.setContentDescription("Image of " + item.getTitle());
            ImageButton directionsButton = v.findViewById(R.id.directionsButton);

            thumb.setOnClickListener(val -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(buildCommonsThumbUrl(item.getThumbUrl(), 600)));
                startActivity(i);
            });

            directionsButton.setOnClickListener(val -> {
                Uri gmm = Uri.parse("geo:" + item.getPosition().latitude + "," + item.getPosition().longitude +
                        "?q=" + Uri.encode(item.getTitle()));
                startActivity(new Intent(Intent.ACTION_VIEW, gmm));
            });

            title.setText(item.getTitle());
            content.setText(empty(item.getSnippet()) ? getString(R.string.no_actual_information) : item.getSnippet());
            content.setMovementMethod(new ScrollingMovementMethod());

            BottomSheetBehavior<?> behavior = dialog.getBehavior();
            behavior.setDraggable(true); // default
            content.setMovementMethod(new ScrollingMovementMethod());
            content.setOnTouchListener(new View.OnTouchListener() {
                float startY;

                @Override
                public boolean onTouch(View tv, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            startY = e.getY();
                            // Start by prioritizing text scrolling
                            behavior.setDraggable(false);
                            tv.getParent().requestDisallowInterceptTouchEvent(true);
                            break;
                        }

                        case MotionEvent.ACTION_MOVE: {
                            float dy = e.getY() - startY;
                            boolean fingerMovingDown = dy > 0;

                            boolean atTop = !tv.canScrollVertically(-1);     // can't scroll up any further

                            // If user is pulling down while already at top, let the sheet drag/dismiss.
                            boolean letSheetDrag = atTop && fingerMovingDown;

                            behavior.setDraggable(letSheetDrag);
                            tv.getParent().requestDisallowInterceptTouchEvent(!letSheetDrag);
                            break;
                        }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            // Restore default
                            behavior.setDraggable(true);
                            tv.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                        }
                    }

                    // IMPORTANT: consume the touch stream (prevents "ACTION_DOWN not received" issues)
                    // and still let the TextView do its internal scrolling.
                    tv.onTouchEvent(e);
                    return true;
                }
            });

            setThumbOrHide(thumb, buildCommonsThumbUrl(item.getThumbUrl(), 600), item.getTitle());

            bindRow(v, R.id.phone, item.getPhone(), val ->
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + val))));

            bindRow(v, R.id.website, item.getUrl(), val ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(val))));

            bindRow(v, R.id.address, item.getAddress(), val -> {
                Uri gmm = Uri.parse("geo:" + item.getPosition().latitude + "," + item.getPosition().longitude +
                        "?q=" + Uri.encode(val));
                startActivity(new Intent(Intent.ACTION_VIEW, gmm));
            });

            bindRow(v, R.id.hours, item.getHours(), null);
            bindRow(v, R.id.price, item.getPrice(), null);

            // If you extract a Wikipedia link out of the listing:
            bindRow(v, R.id.wiki, item.getWikipediaUrl(), val ->
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://en.wikipedia.org/wiki/" + val))));
        }

        dialog.show();
    }

    // Turn "Some Place.jpg" into a 600px-wide URL you can feed to Glide/Picasso
    public static String buildCommonsThumbUrl(String rawName, int widthPx) {
        if (empty(rawName)) {
            return null;
        }

        // Ensure "File:" prefix
        String name = rawName;
        if (!name.regionMatches(true, 0, "File:", 0, 5)) {
            name = "File:" + name;
        }
        // Commons expects underscores
        name = name.replace(' ', '_');

        try {
            // URL-encode the whole title (keep underscores)
            String encoded = java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20");
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encoded + "?width=" + widthPx;
        } catch (Exception e) {
            return null;
        }
    }

    private void bindRow(View root, int textId, @Nullable String value,
            @Nullable java.util.function.Consumer<String> onClick) {
        MaterialTextView tv = root.findViewById(textId);
        if (empty(value)) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(value);
            if (onClick != null) tv.setOnClickListener(v -> onClick.accept(value));
        }
    }

    private void setThumbOrHide(ImageView iv, @Nullable String url, String title) {
        if (!empty(url)) {
            iv.setVisibility(View.VISIBLE);
            iv.setContentDescription(title);
            Glide.with(this).load(url).into(iv);
        } else {
            iv.setVisibility(View.GONE);
            iv.setContentDescription(null);
        }
    }

    private static boolean empty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }


    // ---- WIKIVOYAGE FETCH + CLUSTERING ----
    private void loadNearbyFor(double lat, double lon) {

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            hideLoading();
            return;
        }

        repo.loadNearby(lat, lon, new NearbyCallback() {
            @Override
            public void onSuccess(List<WikiResponse.Page> pages) {
                int pageCount = pages.size();

                Log.d("loadNearbyFor", pageCount + " pages found near" + lat + "," + lon);

                // Pages that DO have coordinates
                List<WikiResponse.Page> pagesWithCoords = pages.stream()
                        .filter(page -> page.coordinates != null)
                        .collect(Collectors.toList());

                // Pages that are missing coordinates (should be rare with colimit, but we log just in case)
                pages.stream()
                        .filter(page -> page.coordinates == null)
                        .forEach(p -> {
                            String message = "Page missing coords: " + p.title;
                            Log.w("loadNearbyFor", message);
                            FirebaseCrashlytics.getInstance().log(message);
                            FirebaseCrashlytics.getInstance().recordException(new Exception(message));
                        });

                updateMapMarkers(new ArrayList<>(pagesWithCoords));
                hideLoading();
                if (pages.isEmpty()) {
                    Toast.makeText(
                            MapActivity.this,R.string.no_articles, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            MapActivity.this,
                            String.format(getResources().getQuantityString(R.plurals.loaded_x_articles, pageCount), pageCount),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(@NonNull Throwable t) {
                hideLoading();
                View root = findViewById(android.R.id.content);
                NetworkErrorHandler.handle(root, (Exception) t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    private void updateMapMarkers(List<WikiResponse.Page> pages) {
        // Clear existing markers/clusters
        clusterManager.clearItems();

        List<PlaceItem> places = new ArrayList<>();
        for (WikiResponse.Page p : pages) {
            if (p.coordinates != null && !p.coordinates.isEmpty()) {
                WikiResponse.Coordinate c = p.coordinates.get(0);
                String snippet = p.extract != null ? MediaWikiUtils.expandSimpleUnits(p.extract) : "";
                String thumb = p.thumbnail != null ? p.thumbnail.source : null;
                places.add(new PlaceItem(
                        c.lat, c.lon, p.title, snippet, thumb, p.pageid, PlaceItem.Kind.ARTICLE));
            }
        }
        clusterManager.addItems(places);
        clusterManager.cluster();
        zoomToBounds(places);
    }

    private void setLocationEnabled() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;
        }
        map.setMyLocationEnabled(true);
    }

    // ---- LOCATION PERMISSION + CENTER ----
    @SuppressLint("MissingPermission")
    private void enableMyLocationAndCenterAndLoad() {
        setLocationEnabled();
        FusedLocationProviderClient fused =
                LocationServices.getFusedLocationProviderClient(this);

        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 11f));
                loadNearbyFor(me.latitude, me.longitude); // 👉 fetch after centering
            } else {
                hideLoading();
            }
        });
    }

    private void mapSightsForPage(PlaceItem currentItem) {

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            return;
        }

        repo.fetchPageWikitext(currentItem.getPageId(), new Callback<>() {
            @Override public void onResponse(@NonNull retrofit2.Call<PageContentResponse> call,
                    @NonNull retrofit2.Response<PageContentResponse> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().query == null
                        || res.body().query.pages == null || res.body().query.pages.isEmpty()) {
                    View root = findViewById(android.R.id.content);
                    Snackbar.make(root, "Server error: " + res.code(), Snackbar.LENGTH_LONG).show();
                    return;
                }
                String wikitext = res.body().query.pages.get(0).revisions.get(0).slots.main.content;

                List<SeeListing> listings = TemplateMatcher.parse(wikitext);
                if (listings.isEmpty()) {
                    Toast.makeText(
                            MapActivity.this,
                            R.string.boring_or_district,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                WikidataCoordsFetcher wikidataFetcher = new WikidataCoordsFetcher();

                List<PlaceItem> initialPins = new ArrayList<>();
                List<SeeListing> missing = new ArrayList<>();

                for (SeeListing s : listings) {
                    Double lat = s.lat();
                    Double lon = s.lon();

                    if (lat != null && lon != null && !Double.isNaN(lat) && !Double.isNaN(lon)) {
                        initialPins.add(toPlaceItem(s, lat, lon, currentItem.getPageId()));
                    } else if (s.wikidata() != null && !s.wikidata().isEmpty()) {
                        missing.add(s);
                    }
                }

                // Add the ones we already have
                clusterManager.addItems(initialPins);
                clusterManager.cluster();

                // Now fetch missing ones
                if (!missing.isEmpty()) {

                    for (SeeListing s : missing) {
                        wikidataFetcher.fetchCoords(s.wikidata(), coords -> {
                            if (coords == null) return;

                            runOnUiThread(() -> {
                                PlaceItem p = toPlaceItem(
                                        s,
                                        coords.latitude,
                                        coords.longitude,
                                        currentItem.getPageId()
                                );
                                clusterManager.addItem(p);
                                clusterManager.cluster();
                            });
                        });
                    }
                }

                // adding current place to pins to make sure we get the correct bounded zoom
                initialPins.add(currentItem);
                zoomToBounds(initialPins);
            }

            @Override public void onFailure(@NonNull retrofit2.Call<PageContentResponse> call, @NonNull Throwable t) {
                Log.w("mapSightsForPage", "failed to load sights for "
                        + currentItem.getTitle() + ": "
                        + t.getLocalizedMessage());
                View root = findViewById(android.R.id.content);
                NetworkErrorHandler.handle(root, (Exception) t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    @Override public void onRequestPermissionsResult(int r, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQ_LOC && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndCenterAndLoad();
        }
    }

    // Zoom to bounds of the new sights (optional, but nice), including the item position
    private void zoomToBounds(List<PlaceItem> places) {
        try {
            if (places.size() == 1) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(places.get(0).getPosition(), 12.0f));
            } else {
                LatLngBounds.Builder latLongBounds = new LatLngBounds.Builder();
                for (PlaceItem place : places) {
                    latLongBounds.include(place.getPosition());
                }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLongBounds.build(), 80));
            }
        } catch (Exception ignored) { /* if only one point, bounds fail; it's fine */ }
    }

    private PlaceItem toPlaceItem(SeeListing s, double lat, double lon, long pageId) {
        return new PlaceItem(
                s.phone(),
                s.url(),
                s.address(),
                s.hours(),
                s.price(),
                s.wikipediaUrl(),
                lat,
                lon,
                s.name(),
                MediaWikiUtils.expandSimpleUnits(s.content()),
                s.thumbUrl(),
                pageId,
                PlaceItem.Kind.SIGHT
        );
    }
}
