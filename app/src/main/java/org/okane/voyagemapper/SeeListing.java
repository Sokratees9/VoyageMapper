package org.okane.voyagemapper;

// SeeListing.java
public class SeeListing {
    public final String name;
    public final Double lat;
    public final Double lon;
    public final String phone;
    public final String url;
    public final String content;

    public SeeListing(String name, Double lat, Double lon, String phone, String url, String content) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.phone = phone;
        this.url = url;
        this.content = content;
    }
}
