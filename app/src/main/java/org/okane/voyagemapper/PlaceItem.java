package org.okane.voyagemapper;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class PlaceItem implements ClusterItem {
    public enum Kind { ARTICLE, SIGHT }
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final String thumbUrl;
    private final long pageId;
    private final Kind kind;

    public PlaceItem(double lat, double lon, String title, String snippet, String thumbUrl, long pageId, Kind kind) {
        this.position = new LatLng(lat, lon);
        this.title = title;
        this.snippet = snippet;
        this.thumbUrl = thumbUrl;
        this.pageId = pageId;
        this.kind = kind;
    }
    @Override public LatLng getPosition() { return position; }
    @Override public String getTitle() { return title; }
    @Override public String getSnippet() { return snippet; }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

    public String getThumbUrl() { return thumbUrl; }
    public long getPageId() { return pageId; }
    public Kind getKind() { return kind; }
}
