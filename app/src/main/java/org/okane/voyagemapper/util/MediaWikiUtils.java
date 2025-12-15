package org.okane.voyagemapper.util;

public class MediaWikiUtils {

    public static String expandSimpleUnits(String text) {
        if (text == null || text.isEmpty()) return text;

        // {{km|12}} -> 12 km
        text = text.replaceAll("\\{\\{\\s*km\\s*\\|\\s*(.*?)\\s*\\}\\}", "$1 km");

        // {{m|300}} -> 300 m
        text = text.replaceAll("\\{\\{\\s*m\\s*\\|\\s*(.*?)\\s*\\}\\}", "$1 m");

        return text;
    }
}
