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

    public static void handle(View view, Throwable t) {
        Resources resources = view.getResources();
        // Fallback for unexpected exceptions
        String message = resources.getString(R.string.an_error_occurred);

        if (t instanceof UnknownHostException) {
            // No network access / DNS failed
            message = resources.getString(R.string.no_internet_connection);
        } else if (t instanceof ConnectException) {
            message = resources.getString(R.string.unable_to_reach_server);
        } else if (t instanceof SocketTimeoutException) {
            message = resources.getString(R.string.connection_timed_out);
        } else if (t instanceof ApiException apiException) {
            if (apiException.getStatusCode() == 429) {
                message = resources.getString(R.string.too_many_requests);
            } else {
                message = resources.getString(R.string.an_error_occurred) + ": " + apiException.getStatusCode();
            }
        } else if (t instanceof IOException) {
            message = resources.getString(R.string.network_error_occurred);
        }
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        Log.e("NetworkErrorHandler", message, t);
    }
}
