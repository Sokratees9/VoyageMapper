package org.okane.voyagemapper.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class SimpleUtils {

    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    public static void startUrlActivity(int resId, Context context) {
        String url = context.getString(resId);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No app found to load URL: " + url, Toast.LENGTH_SHORT).show();
        }
    }
}
