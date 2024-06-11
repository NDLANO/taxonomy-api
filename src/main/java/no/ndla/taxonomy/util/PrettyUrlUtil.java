/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import java.util.Optional;
import no.ndla.taxonomy.domain.LanguageField;
import no.ndla.taxonomy.domain.NodeType;
import org.jsoup.Jsoup;

public class PrettyUrlUtil {

    public static Optional<String> createPrettyUrl(
            Optional<LanguageField<String>> rootName,
            LanguageField<String> name,
            String language,
            String hash,
            NodeType nodeType,
            boolean newUrlSeparator) {
        return createPrettyUrl(
                rootName.map(lf -> lf.fromLanguage(language)),
                name.fromLanguage(language),
                hash,
                nodeType,
                newUrlSeparator);
    }

    public static Optional<String> createPrettyUrl(
            Optional<String> rootName, String name, String hash, NodeType nodeType, boolean newUrlSeparator) {
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
        if (newUrlSeparator) {
            builder.append(String.format("%s/%s", nodeTypeMapping(nodeType), hash));
        } else {
            builder.append(String.format("__%s", hash));
        }

        return Optional.of(builder.toString());
    }

    private static String nodeTypeMapping(NodeType nodeType) {
        return switch (nodeType) {
            case SUBJECT -> "/f";
            case TOPIC -> "/e";
            case RESOURCE -> "/r";
            default -> "";
        };
    }

    private static void buildUrlFragment(StringBuilder builder, String text) {
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append("-");
            }
            builder.append(words[i]);
        }
    }

    private static String cleanString(String name) {
        return Jsoup.parse(name)
                .text()
                .toLowerCase()
                .replaceAll("[.,!?\\-()/]", "")
                .replaceAll("æ", "a")
                .replaceAll("ø", "o")
                .replaceAll("å", "a")
                .trim();
    }

    public static String getHashFromPath(String title) {
        if (title.contains("__")) {
            return title.split("__")[1];
        }
        // titles with hash will have a /r/ or /e/ or /f/ in the path
        if (!title.contains("/r/") && !title.contains("/e/") && !title.contains("/f/")) {
            return "";
        }
        return title.substring(title.lastIndexOf("/") + 1);
    }
}
