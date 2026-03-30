package org.okane.voyagemapper;

import static org.okane.voyagemapper.util.MediaWikiUtils.expandSimpleUnits;
import static org.okane.voyagemapper.util.MediaWikiUtils.fixDerry;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.maps.android.clustering.ClusterManager;

import org.okane.voyagemapper.data.ArticleRepository;
import org.okane.voyagemapper.data.ListingRepository;
import org.okane.voyagemapper.data.local.AppDatabase;
import org.okane.voyagemapper.data.local.dao.CachedArticleDao;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;
import org.okane.voyagemapper.service.NetworkChecker;
import org.okane.voyagemapper.ui.model.PlaceItem;
import org.okane.voyagemapper.model.SeeListing;
import org.okane.voyagemapper.service.NearbyCallback;
import org.okane.voyagemapper.service.NetworkErrorHandler;
import org.okane.voyagemapper.service.WikiRepository;
import org.okane.voyagemapper.service.WikiResponse;
import org.okane.voyagemapper.ui.PlaceClusterRenderer;
import org.okane.voyagemapper.ui.sheet.PlaceSheetController;
import org.okane.voyagemapper.util.NetworkUtils;
import org.okane.voyagemapper.util.WikidataCoordsFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, NetworkChecker {

    private GoogleMap map;
    private View loadingOverlay;
    private TextView loadingText;
    private ClusterManager<PlaceItem> clusterManager;
    private final WikiRepository repo = new WikiRepository();
    private PlaceItem pendingSavedArticle;
    private Marker pendingSavedMarker;
    private ListingRepository listingRepository;
    private ArticleRepository articleRepository;
    private PlaceSheetController placeSheetController;
    private CachedArticleDao articleDao;
    private final ExecutorService diskIo = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int REQ_LOC = 42;
    public static final String EXTRA_OPEN_SAVED_ARTICLE = "open_saved_article";
    public static final String EXTRA_PAGE_ID = "page_id";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SNIPPET = "snippet";
    public static final String EXTRA_THUMB_URL = "thumb_url";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AppDatabase db = AppDatabase.getInstance(this);
        articleDao = db.cachedArticleDao();
        articleRepository = new ArticleRepository(db.cachedArticleDao(), diskIo);
        listingRepository = new ListingRepository(db.cachedSeeListingDao(), diskIo, repo, this);

        placeSheetController = new PlaceSheetController(this, new PlaceSheetController.Callbacks() {
            @Override
            public void onArticleViewed(@NonNull PlaceItem item) {
                articleRepository.markArticleViewed(item);
            }

            @Override
            public void onToggleSavedArticle(@NonNull PlaceItem item, @NonNull ImageButton button) {
                toggleSavedArticle(item, button);
            }

            @Override
            public void onUpdateSavedArticleIcon(long pageId, @NonNull ImageButton button) {
                updateSaveArticleIcon(pageId, button);
            }

            @Override
            public void onMapSightsRequested(@NonNull PlaceItem item) {
                mapSightsForPage(item);
            }

            @Override
            public void onPrefetchListingsRequested(@NonNull PlaceItem item) {
                listingRepository.prefetchListingsForArticle(item);
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_SAVED_ARTICLE, false)) {
            long pageId = intent.getLongExtra(EXTRA_PAGE_ID, 0L);
            double lat = intent.getDoubleExtra(EXTRA_LAT, 0d);
            double lon = intent.getDoubleExtra(EXTRA_LON, 0d);
            String title = intent.getStringExtra(EXTRA_TITLE);
            String snippet = intent.getStringExtra(EXTRA_SNIPPET);
            String thumbUrl = intent.getStringExtra(EXTRA_THUMB_URL);

            pendingSavedArticle = new PlaceItem(
                    lat,
                    lon,
                    title != null ? title : "",
                    snippet != null ? snippet : "",
                    thumbUrl,
                    pageId,
                    PlaceItem.Kind.ARTICLE
            );
        }
        articleRepository.pruneOldCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        diskIo.shutdown();
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

        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        if ("CENTER_AT".equals(mode)) {
            showLoading(getString(R.string.loading_articles));
            double lat = intent.getDoubleExtra("lat", fallback.latitude);
            double lon = intent.getDoubleExtra("lon", fallback.longitude);
            LatLng target = new LatLng(lat, lon);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 11f));
            setLocationEnabled();
//            String title = intent.getStringExtra("title");
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

        } else if (pendingSavedArticle != null) {
            showLoading(getString(R.string.loading_articles));
            PlaceItem savedArticle = pendingSavedArticle;
            openPendingSavedArticle();
            loadNearbyFor(
                    savedArticle.getPosition().latitude,
                    savedArticle.getPosition().longitude
            );
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

    private void openPendingSavedArticle() {
        if (map == null || pendingSavedArticle == null) {
            return;
        }

        LatLng latLng = new LatLng(
                pendingSavedArticle.getPosition().latitude,
                pendingSavedArticle.getPosition().longitude
        );

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f));

        if (pendingSavedMarker != null) {
            pendingSavedMarker.remove();
        }

        pendingSavedMarker = map.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title(pendingSavedArticle.getTitle())
        );

        placeSheetController.showPlaceSheet(pendingSavedArticle);
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
            placeSheetController.showPlaceSheet(item);
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

    private void loadNearbyFromCache(double lat, double lon) {
        double delta = 0.25; // roughly a local area; tune later

        double minLat = lat - delta;
        double maxLat = lat + delta;
        double minLon = lon - delta;
        double maxLon = lon + delta;

        diskIo.execute(() -> {
            List<CachedArticleEntity> cached = articleDao.getArticlesInBounds(minLat, maxLat, minLon, maxLon);

            mainHandler.post(() -> {
                hideLoading();

                if (cached == null || cached.isEmpty()) {
                    Toast.makeText(
                            MapActivity.this,
                            "No internet connection and no cached articles for this area",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                updateMapMarkersFromCache(cached);

                int count = cached.size();
                Toast.makeText(
                        MapActivity.this,
                        "Showing " + count + " cached article" + (count == 1 ? "" : "s"),
                        Toast.LENGTH_LONG
                ).show();
            });
        });
    }

    private void updateMapMarkersFromCache(List<CachedArticleEntity> cachedArticles) {
        List<PlaceItem> places = new ArrayList<>();

        for (CachedArticleEntity a : cachedArticles) {
            places.add(new PlaceItem(
                    a.lat,
                    a.lon,
                    a.title,
                    a.snippet != null ? a.snippet : "",
                    a.thumbUrl,
                    a.pageId,
                    PlaceItem.Kind.ARTICLE
            ));
        }
        updateClusteredMarkers(places);
    }

    private void updateClusteredMarkers(List<PlaceItem> places) {
        // Clear existing markers/clusters
        clusterManager.clearItems();
        clusterManager.addItems(places);
        clusterManager.cluster();
        zoomToBounds(places);
    }

    private void prefetchListingsForArticles(List<PlaceItem> places) {
        if (places == null || places.isEmpty()) {
            return;
        }

        int prefetched = 0;
        for (PlaceItem place : places) {
            if (place.getKind() == PlaceItem.Kind.ARTICLE) {
                listingRepository.prefetchListingsForArticle(place);
                prefetched++;
                if (prefetched >= 20) {
                    break;
                }
            }
        }
    }

    // ---- WIKIVOYAGE FETCH + CLUSTERING ----
    private void loadNearbyFor(double lat, double lon) {
        if (!isNetworkAvailable()) {
            loadNearbyFromCache(lat, lon);
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
                Log.e("loadNearbyFor", "Nearby fetch failed, trying cache", t);
                FirebaseCrashlytics.getInstance().recordException(t);
                loadNearbyFromCache(lat, lon);
            }
        });
    }

    private void updateMapMarkers(List<WikiResponse.Page> pages) {
        List<PlaceItem> places = new ArrayList<>();
        for (WikiResponse.Page p : pages) {
            if (p.coordinates != null && !p.coordinates.isEmpty()) {
                WikiResponse.Coordinate c = p.coordinates.get(0);
                String snippet = p.extract != null ?
                        fixDerry(expandSimpleUnits(p.extract), p.title) : "";
                String thumb = p.thumbnail != null ? p.thumbnail.source : null;
                PlaceItem placeItem = new PlaceItem(
                        c.lat, c.lon, fixDerry(p.title, p.title), snippet, thumb, p.pageid, PlaceItem.Kind.ARTICLE);
                places.add(placeItem);
                articleRepository.cacheArticleSummary(placeItem);
            }
        }
        updateClusteredMarkers(places);
        prefetchListingsForArticles(places);
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
        listingRepository.getCachedListingsForPage(currentItem.getPageId(), new ListingRepository.ListingsCallback() {
            @Override
            public void onSuccess(List<SeeListing> cachedListings) {
                runOnUiThread(() -> {
                    if (cachedListings != null && !cachedListings.isEmpty()) {
                        showListingsOnMap(currentItem, cachedListings);
                        return;
                    }

                    if (isNetworkAvailable()) {
                        Toast.makeText(
                                MapActivity.this,
                                "No internet connection and no cached sights available",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    listingRepository.fetchListingsForPage(currentItem.getPageId(), new ListingRepository.ListingsCallback() {
                        @Override
                        public void onSuccess(List<SeeListing> listings) {
                            runOnUiThread(() -> {
                                if (listings.isEmpty()) {
                                    Toast.makeText(
                                            MapActivity.this,
                                            R.string.boring_or_district,
                                            Toast.LENGTH_LONG
                                    ).show();
                                    return;
                                }

                                listingRepository.cacheListingsForArticle(currentItem.getPageId(), listings);
                                showListingsOnMap(currentItem, listings);
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            runOnUiThread(() -> {
                                Log.w("mapSightsForPage", "failed to load sights for "
                                        + currentItem.getTitle() + ": "
                                        + t.getLocalizedMessage());
                                View root = findViewById(android.R.id.content);
                                NetworkErrorHandler.handle(root, (Exception) t);
                                FirebaseCrashlytics.getInstance().recordException(t);
                            });
                        }
                    });
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> Log.w("mapSightsForPage", "failed to read cached sights for "
                        + currentItem.getTitle() + ": "
                        + t.getLocalizedMessage()));
            }
        });
    }

    private void showListingsOnMap(PlaceItem currentItem, List<SeeListing> listings) {
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

        clusterManager.addItems(initialPins);
        clusterManager.cluster();

        if (!missing.isEmpty()) {
            for (SeeListing s : missing) {
                wikidataFetcher.fetchCoords(s.wikidata(), coords -> {
                    if (coords == null) return;

                    diskIo.execute(() -> listingRepository.updateCoordsForListing(
                            currentItem.getPageId(),
                            s.name(),
                            coords.latitude,
                            coords.longitude
                    ));

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

        initialPins.add(currentItem);
        zoomToBounds(initialPins);
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
        PlaceItem placeItem =  new PlaceItem(
                s.phone(),
                s.url(),
                fixDerry(s.address(), s.name()),
                s.hours(),
                s.price(),
                s.wikipediaUrl(),
                lat,
                lon,
                fixDerry(s.name(), s.name()),
                expandSimpleUnits(s.content()),
                s.thumbUrl(),
                pageId,
                PlaceItem.Kind.SIGHT
        );
        articleRepository.cacheArticleSummary(placeItem);
        return placeItem;
    }

    private void updateSaveArticleIcon(long pageId, ImageButton button) {
        articleRepository.isSaved(pageId, isSaved ->
                mainHandler.post(() -> {
                    button.setSelected(isSaved);
                    button.setImageResource(
                            isSaved ? R.drawable.ic_bookmark_added : R.drawable.ic_bookmark_add
                    );
                    button.setContentDescription(
                            getString(isSaved ? R.string.saved_article : R.string.save_article)
                    );
                })
        );
    }

    private void toggleSavedArticle(PlaceItem item, ImageButton button) {
        if (item == null || item.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        button.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                .withEndAction(() -> button.animate().scaleX(1f).scaleY(1f).setDuration(100));

        articleRepository.getArticleByPageId(item.getPageId(), new ArticleRepository.ArticleCallback() {
            @Override
            public void onSuccess(CachedArticleEntity cached) {
                boolean isSaved = cached != null && cached.isSaved;

                if (isSaved) {
                    articleRepository.setSaved(item.getPageId(), false);
                    listingRepository.deleteListingsForPage(item.getPageId());

                    mainHandler.post(() -> {
                        Toast.makeText(MapActivity.this, "Offline article removed", Toast.LENGTH_SHORT).show();
                        updateSaveArticleIcon(item.getPageId(), button);
                    });
                } else {
                    saveArticleAndListings(item, button);
                }
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    Toast.makeText(MapActivity.this,
                            "Could not update saved article", Toast.LENGTH_SHORT).show();
                    Log.e("MapActivity", "Could not update saved article: " + t.getMessage());
                });
            }
        });
    }

    private void saveArticleAndListings(PlaceItem item, ImageButton button) {
        if (item == null || item.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        articleRepository.saveArticleRecord(item);

        listingRepository.fetchListingsForPage(item.getPageId(), new ListingRepository.ListingsCallback() {
            @Override
            public void onSuccess(List<SeeListing> listings) {
                listingRepository.cacheListingsForArticle(item.getPageId(), listings);

                mainHandler.post(() -> {
                    Toast.makeText(
                            MapActivity.this,
                            item.getTitle() + getString(R.string.sights_saved),
                            Toast.LENGTH_SHORT
                    ).show();
                    updateSaveArticleIcon(item.getPageId(), button);
                });
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    Toast.makeText(
                            MapActivity.this,
                            "Article saved, but sights could not be downloaded",
                            Toast.LENGTH_LONG
                    ).show();
                    updateSaveArticleIcon(item.getPageId(), button);
                });
            }
        });
    }

    @Override
    public boolean isNetworkAvailable() {
        return NetworkUtils.isNetworkAvailable(MapActivity.this);
    }
}
