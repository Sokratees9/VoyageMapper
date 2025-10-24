package org.okane.voyagemapper;

public class PlaceResult {
    public final String title;
    public final double lat;
    public final double lon;

    public PlaceResult(String title, double lat, double lon) {
        this.title = title; this.lat = lat; this.lon = lon;
    }
}
