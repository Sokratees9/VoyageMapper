package org.okane.voyagemapper.model;

/**
 * A parsed data object from Wikivoyage content.
 *
 * @param name
 * @param lat
 * @param lon
 * @param phone
 * @param url
 * @param content
 * @param address
 * @param hours
 * @param price
 * @param wikipediaUrl
 * @param wikidata
 * @param thumbUrl
 */
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
