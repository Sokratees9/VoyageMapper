package org.okane.voyagemapper;

// SeeListing.java
public class SeeListing {
    public final String name;
    public final Double lat;
    public final Double lon;
    public final String phone;
    public final String url;
    public final String content;
    public final String address;
    public final String hours;
    public final String price;
    public final String wikipediaUrl;
    public final String thumbUrl;

    public SeeListing(String name, Double lat, Double lon, String phone, String url, String content, String address,
            String hours, String price, String wikipediaUrl, String thumbUrl) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.phone = phone;
        this.url = url;
        this.content = content;
        this.address = address;
        this.hours = hours;
        this.price = price;
        this.wikipediaUrl = wikipediaUrl;
        this.thumbUrl = thumbUrl;
    }
}
