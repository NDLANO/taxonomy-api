/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import java.util.Optional;
import org.jsoup.Jsoup;

public class TitleUtil {

    public static Optional<String> createPrettyUrl(String name, String hash, Optional<String> rootName) {
        if (name == null || hash == null) return Optional.empty();
        StringBuilder builder = new StringBuilder();
        rootName.ifPresent(rn -> {
            if (!rn.equals(name)) {
                builder.append("/");
                buildUrlFragment(builder, cleanString(rootName.get()));
            }
        });
        builder.append("/");
        buildUrlFragment(builder, cleanString(name));
        builder.append(String.format("__%s", hash));
        return Optional.of(builder.toString());
    }

    private static void buildUrlFragment(StringBuilder builder, String text) {
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append("-");
            }
            builder.append(words[i].toLowerCase());
        }
    }

    private static String cleanString(String name) {
        return Jsoup.parse(name)
                .text()
                .replaceAll("[.,!?\\-()]", "")
                .replaceAll("æ", "a")
                .replaceAll("ø", "o")
                .replaceAll("å", "a")
                .trim();
    }

    public static String getHashFromPath(String title) {
        if (!title.contains("__")) {
            return "";
        }
        return title.split("__")[1];
    }
}
