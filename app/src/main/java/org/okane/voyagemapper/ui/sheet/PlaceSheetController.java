package org.okane.voyagemapper.ui.sheet;

import static org.okane.voyagemapper.util.SimpleUtils.isEmpty;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.okane.voyagemapper.MapActivity;
import org.okane.voyagemapper.R;
import org.okane.voyagemapper.ui.model.PlaceItem;

public class PlaceSheetController {

    public interface Callbacks {
        void onArticleViewed(@NonNull PlaceItem item);
        void onToggleSavedArticle(@NonNull PlaceItem item, @NonNull ImageButton button);
        void onUpdateSavedArticleIcon(long pageId, @NonNull ImageButton button);
        void onMapSightsRequested(@NonNull PlaceItem item);
        void onPrefetchListingsRequested(@NonNull PlaceItem item);
    }

    private final MapActivity activity;
    private final Callbacks callbacks;

    public PlaceSheetController(@NonNull MapActivity activity, @NonNull Callbacks callbacks) {
        this.activity = activity;
        this.callbacks = callbacks;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void showPlaceSheet(@NonNull PlaceItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        if (item.getKind() == PlaceItem.Kind.ARTICLE) {
            showArticleSheet(dialog, item);
        } else {
            showSightSheet(dialog, item);
        }
        dialog.show();
    }

    private void showArticleSheet(@NonNull BottomSheetDialog dialog, @NonNull PlaceItem item) {
        callbacks.onArticleViewed(item);

        @SuppressLint("InflateParams")
        View v = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_article, null);
        dialog.setContentView(v);

        TextView title = v.findViewById(R.id.title);
        TextView snippet = v.findViewById(R.id.snippet);
        ImageView thumb = v.findViewById(R.id.thumb);
        thumb.setContentDescription(activity.getString(R.string.image_of) + item.getTitle());
        MaterialButton openBtn = v.findViewById(R.id.openButton);
        MaterialButton mapSightsBtn = v.findViewById(R.id.mapSightsButton);
        ImageButton saveArticleBtn = v.findViewById(R.id.saveArticleButton);
        saveArticleBtn.setOnClickListener(btn -> callbacks.onToggleSavedArticle(item, saveArticleBtn));
        callbacks.onUpdateSavedArticleIcon(item.getPageId(), saveArticleBtn);

        thumb.setOnClickListener(val -> {
            String thumbUrl = item.getThumbUrl();
            if (thumbUrl == null) {
                return;
            }

            // 1) remove '/thumb/' segment
            String noThumb = thumbUrl.replace("/thumb/", "/");
            // 2) strip the trailing '/<width>px-<filename>' segment entirely
            // e.g. '/250px-Colosseo_2020.jpg' at the end
            String fullUrl = noThumb.replaceAll("/\\d+px-[^/]+$", "");

            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)));
        });

        title.setText(item.getTitle());
        setThumbOrHide(thumb, item.getThumbUrl(), item.getTitle());
        snippet.setText(isEmpty(item.getSnippet())
                        ? activity.getString(R.string.no_actual_information)
                        : item.getSnippet()
        );

        openBtn.setOnClickListener(b -> {
            String url = "https://en.wikivoyage.org/?curid=" + item.getPageId();
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        mapSightsBtn.setOnClickListener(b -> {
            dialog.dismiss();
            callbacks.onMapSightsRequested(item);
        });

        callbacks.onPrefetchListingsRequested(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showSightSheet(@NonNull BottomSheetDialog dialog, @NonNull PlaceItem item) {
        @SuppressLint("InflateParams")
        View v = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_sight, null);
        dialog.setContentView(v);

        TextView title = v.findViewById(R.id.title);
        TextView content = v.findViewById(R.id.content);
        ImageView thumb = v.findViewById(R.id.thumb);
        thumb.setContentDescription(activity.getString(R.string.image_of) + item.getTitle());
        ImageButton directionsButton = v.findViewById(R.id.directionsButton);

        thumb.setOnClickListener(val -> {
            Intent i = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(buildCommonsThumbUrl(item.getThumbUrl()))
            );
            activity.startActivity(i);
        });

        directionsButton.setOnClickListener(val -> {
            Uri gmm = Uri.parse(
                    "geo:" + item.getPosition().latitude + "," + item.getPosition().longitude +
                            "?q=" + Uri.encode(item.getTitle())
            );
            activity.startActivity(new Intent(Intent.ACTION_VIEW, gmm));
        });

        title.setText(item.getTitle());
        content.setText(isEmpty(item.getSnippet())
                        ? activity.getString(R.string.no_actual_information)
                        : item.getSnippet()
        );
        content.setMovementMethod(new ScrollingMovementMethod());

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setDraggable(true);
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
                        boolean atTop = !tv.canScrollVertically(-1);   // can't scroll up any further
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

        setThumbOrHide(thumb, buildCommonsThumbUrl(item.getThumbUrl()), item.getTitle());

        bindRow(v, R.id.phone, item.getPhone(), val ->
                activity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + val))));

        bindRow(v, R.id.website, item.getUrl(), val ->
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(val))));

        bindRow(v, R.id.address, item.getAddress(), val -> {
            Uri gmm = Uri.parse(
                    "geo:" + item.getPosition().latitude + "," + item.getPosition().longitude +
                            "?q=" + Uri.encode(val)
            );
            activity.startActivity(new Intent(Intent.ACTION_VIEW, gmm));
        });

        bindRow(v, R.id.hours, item.getHours(), null);
        bindRow(v, R.id.price, item.getPrice(), null);

        // If you extract a Wikipedia link out of the listing:
        bindRow(v, R.id.wiki, item.getWikipediaUrl(), val ->
                activity.startActivity(
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://en.wikipedia.org/wiki/" + val))));
    }

    // Turn "Some Place.jpg" into a 600px-wide URL you can feed to Glide/Picasso
    private static String buildCommonsThumbUrl(String rawName) {
        if (isEmpty(rawName)) {
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
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encoded + "?width=" + 600;
        } catch (Exception e) {
            return null;
        }
    }

    private void setThumbOrHide(ImageView iv, @Nullable String url, String title) {
        if (!isEmpty(url)) {
            iv.setVisibility(View.VISIBLE);
            iv.setContentDescription(title);
            Glide.with(activity).load(url).into(iv);
        } else {
            iv.setVisibility(View.GONE);
            iv.setContentDescription(null);
        }
    }

    private void bindRow(View root, int textId, @Nullable String value,
            @Nullable java.util.function.Consumer<String> onClick) {
        MaterialTextView tv = root.findViewById(textId);
        if (isEmpty(value)) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(value);
            if (onClick != null) tv.setOnClickListener(v -> onClick.accept(value));
        }
    }
}
