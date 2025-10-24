package org.okane.voyagemapper;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.HashMap;
import java.util.Map;

public class PlaceClusterRenderer extends DefaultClusterRenderer<PlaceItem> {

    private final MarkerBitmapFactory articleFactory;
    private final MarkerBitmapFactory sightFactory;

    private final Map<String, Bitmap> articleCache = new HashMap<>();
    private final Map<String, Bitmap> sightCache   = new HashMap<>();

    public PlaceClusterRenderer(Context ctx, GoogleMap map, ClusterManager<PlaceItem> cm) {
        super(ctx, map, cm);
        articleFactory = new MarkerBitmapFactory(ctx, R.layout.marker_label_article);
        sightFactory   = new MarkerBitmapFactory(ctx, R.layout.marker_label_sight);
    }

    @Override
    protected void onBeforeClusterItemRendered(PlaceItem item, MarkerOptions markerOptions) {
        boolean isSight = item.getKind() == PlaceItem.Kind.SIGHT;
        String key = (isSight ? "S|" : "A|") + (item.getTitle() == null ? "" : item.getTitle());

        Bitmap bmp = (isSight ? sightCache : articleCache).get(key);
        if (bmp == null) {
            bmp = (isSight ? sightFactory : articleFactory).make(item.getTitle());
            (isSight ? sightCache : articleCache).put(key, bmp);
        }

        markerOptions
                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                .anchor(0.5f, 1.0f)
                .title(item.getTitle())
                .snippet(item.getSnippet());
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<PlaceItem> cluster) {
        // Same rule for both kinds; tweak if you want different cluster thresholds
        return cluster.getSize() >= 3;
    }
}