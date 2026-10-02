package org.okane.voyagemapper;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.libraries.places.api.Places;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoyageMapApplication extends Application {

    private final ExecutorService diskIo = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();

        String apiKey = readMapsKeyFromManifest();
        Log.d("VoyageMapper", "Maps/Places key prefix: " + (apiKey != null && apiKey.length() >= 8 ? apiKey.substring(0,8) : "NULL"));
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Google Maps/Places API key not found in manifest");
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
        }
    }

    public ExecutorService getDiskIo() {
        return diskIo;
    }

    private String readMapsKeyFromManifest() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle b = ai.metaData;
            return b.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            return null;
        }
    }
}