package org.okane.voyagemapper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.okane.voyagemapper.model.PlaceResult;
import org.okane.voyagemapper.service.NetworkErrorHandler;
import org.okane.voyagemapper.ui.OnboardingBottomSheetDialogFragment;
import org.okane.voyagemapper.ui.PlaceResultAdapter;
import org.okane.voyagemapper.ui.SavedArticlesActivity;
import org.okane.voyagemapper.util.NetworkUtils;
import org.okane.voyagemapper.util.SimpleUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SharedPreferences p = getSharedPreferences("voyage_prefs", MODE_PRIVATE);
        boolean seen = p.getBoolean("onboarding_seen", false);

        if (!seen) {
            new OnboardingBottomSheetDialogFragment()
                    .show(getSupportFragmentManager(), "onboarding");
        }

        View root = findViewById(R.id.root); // your top-level container in activity_main.xml
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        placesClient = Places.createClient(this);

        findViewById(R.id.useCurrentLocationBtn).setOnClickListener(v -> {
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("mode", "MY_LOCATION");
            startActivity(i);
        });

        searchEditText = findViewById(R.id.searchEditText);
        searchPrompt = findViewById(R.id.searchPrompt);
        resultsRecycler = findViewById(R.id.resultsRecycler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(this));

        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.main_overflow_menu, popup.getMenu());
            popup.setForceShowIcon(true);

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.menu_saved_articles) {
                    startActivity(new Intent(MainActivity.this, SavedArticlesActivity.class));
                    return true;
                } else if (id == R.id.menu_info) {
                    new OnboardingBottomSheetDialogFragment()
                            .show(getSupportFragmentManager(), "onboarding");
                    return true;
                } else if (id == R.id.menu_about) {
                    showAboutDialog();
                    return true;
                }

                return false;
            });

            popup.show();
        });

        adapter = new PlaceResultAdapter(item -> {
            if (item.placeId() == null) {
                Log.w("MainActivity", "No placeId for " + item);
                return;
            }

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
                return;
            }

            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.LOCATION,
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.WEBSITE_URI,
                    Place.Field.INTERNATIONAL_PHONE_NUMBER
            );

            FetchPlaceRequest req = FetchPlaceRequest.newInstance(item.placeId(), fields);

            placesClient.fetchPlace(req).addOnSuccessListener(response -> {
                Place place = response.getPlace();
                if (place.getLocation() == null) {
                    return;
                }

                double lat = place.getLocation().latitude;
                double lon = place.getLocation().longitude;

                // Launch your MapActivity centered here (your existing flow)
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                i.putExtra("mode", "CENTER_AT");
                i.putExtra("lat", lat);
                i.putExtra("lon", lon);
//                i.putExtra("title", place.getName());
                startActivity(i);
            }).addOnFailureListener(err -> {
                Log.e("MainActivity", "Places fetch failed", err);
                NetworkErrorHandler.handle(findViewById(android.R.id.content), err);
            });
        });
        resultsRecycler.setAdapter(adapter);

        findViewById(R.id.searchButton).setOnClickListener(v -> searchAndHideKeyboard());
        // Also handle keyboard search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAndHideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void searchAndHideKeyboard() {
        doSearch();
        hideKeyboard();
        searchEditText.clearFocus();
    }

    private void doSearch() {
        String q = String.valueOf(searchEditText.getText()).trim();
        if (q.isEmpty()) {
            Toast.makeText(this, "Enter a place to search", Toast.LENGTH_SHORT).show();
            return;
        }
        searchPlaces(q);
    }

    private void searchPlaces(String query) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            return;
        }

        // One token per “search session” helps billing & quality
        AutocompleteSessionToken token =
                AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest req =
                FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setTypesFilter(Collections.singletonList(
                                PlaceTypes.LOCALITY
                        ))
                        .setSessionToken(token)
                        .build();

        placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener(resp -> {
                    List<PlaceResult> results = new ArrayList<>();

                    if (resp.getAutocompletePredictions().isEmpty()) {
                        showResults(Collections.emptyList(), false);
                        Toast.makeText(this, "No results found for " + query, Toast.LENGTH_SHORT).show();
                    }

                    for (AutocompletePrediction p : resp.getAutocompletePredictions()) {
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
                    Log.w("MainActivity", "Places search failed", e);
                    showResults(Collections.emptyList(), false);
                    NetworkErrorHandler.handle(findViewById(android.R.id.content), e);
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

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

        TextView versionText = view.findViewById(R.id.aboutVersion);
        TextView emailText = view.findViewById(R.id.aboutEmail);
        TextView privacyPolicyText = view.findViewById(R.id.aboutPrivacyPolicy);
        TextView howToUseText = view.findViewById(R.id.aboutHowToUse);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
            versionText.setText(getString(R.string.about_version_value, versionName, versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity", "Failed to get package info", e);
            versionText.setText(R.string.version_unknown);
        }

        emailText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + R.string.email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Voyage Map");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        privacyPolicyText.setOnClickListener(v ->
                SimpleUtils.startUrlActivity(R.string.privacy_html, this)
        );

        howToUseText.setOnClickListener(v ->
                SimpleUtils.startUrlActivity(R.string.how_it_works_html, this)
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle("About Voyage Map")
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }
}