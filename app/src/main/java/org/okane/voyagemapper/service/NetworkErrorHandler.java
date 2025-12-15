package org.okane.voyagemapper.service;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.okane.voyagemapper.R;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;

public class NetworkErrorHandler {

    public static void handle(View view, Exception e) {
        Resources resources = view.getResources();
        // Fallback for unexpected exceptions
        String message = resources.getString(R.string.an_error_occurred);

        if (e instanceof UnknownHostException) {
            // No network access / DNS failed
            message = resources.getString(R.string.no_internet_connection);
        } else if (e instanceof ConnectException) {
            message = resources.getString(R.string.unable_to_reach_server);
        } else if (e instanceof SocketTimeoutException) {
            message = resources.getString(R.string.connection_timed_out);
        } else if (e instanceof IOException) {
            message = resources.getString(R.string.network_error_occurred);
        }
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        Log.e("NetworkErrorHandler", message, e);
    }
}
