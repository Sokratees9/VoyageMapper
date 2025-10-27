package org.okane.voyagemapper;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class PlaceItem implements ClusterItem {
    private final String phone;
    private final String url;
    private final String address;
    private final String hours;
    private final String price;
    private final String wikipediaUrl;

    public enum Kind { ARTICLE, SIGHT }
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final String thumbUrl;
    private final long pageId;
    private final Kind kind;

    public PlaceItem(String phone, String url, String address, String hours, String price, String wikipediaUrl, double lat, double lon, String title, String snippet, String thumbUrl, long pageId, Kind kind) {
        this.phone = phone;
        this.url = url;
        this.address = address;
        this.hours = hours;
        this.price = price;
        this.wikipediaUrl = wikipediaUrl;
        this.position = new LatLng(lat, lon);
        this.title = title;
        this.snippet = snippet;
        this.thumbUrl = thumbUrl;
        this.pageId = pageId;
        this.kind = kind;
    }

    public PlaceItem(double lat, double lon, String title, String snippet, String thumbUrl, long pageId, Kind kind) {
        this(null, null, null, null, null, null, lat, lon, title, snippet, thumbUrl, pageId, kind);
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
    public String getPhone() { return phone; }
    public String getUrl() { return url; }
    public String getAddress() { return address; }
    public String getHours() { return hours; }
    public String getPrice() { return price; }
    public String getWikipediaUrl() { return wikipediaUrl; }
}
