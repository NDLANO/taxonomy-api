/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import org.jsoup.Jsoup;

import java.util.Optional;

public class TitleUtil {

    public static Optional<String> createPrettyUrl(String source, String hash) {
        if (source == null || hash == null)
            return Optional.empty();
        var text = Jsoup.parse(source).text();
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder("/");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append("-");
            }
            sb.append(words[i].toLowerCase());
        }
        sb.append(String.format("__%s", hash));
        return Optional.of(sb.toString());
    }

    public static String getHashFromPath(String title) {
        if (!title.contains("__")) {
            return "";
        }
        return title.split("__")[1];
    }
}
