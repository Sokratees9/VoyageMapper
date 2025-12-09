package org.okane.voyagemapper.model;

// SeeListing.java
public record SeeListing (
        String name,
        Double lat,
        Double lon,
        String phone,
        String url,
        String content,
        String address,
        String hours,
        String price,
        String wikipediaUrl,
        String wikidata,
        String thumbUrl) { }
