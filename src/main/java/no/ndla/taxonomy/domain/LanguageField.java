/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.config.Constants;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageField<V> extends HashMap<String, V> {
    public LanguageField() {
    }

    public static LanguageField<String> fromNode(DomainObject node) {
        var languageField = new LanguageField<String>();
        languageField.put("nb", node.getName());

        node.getTranslations().forEach(nt -> {
            languageField.put(nt.getLanguageCode(), nt.getName());
        });

        return languageField;
    }

    public static LanguageField<List<String>> listFromNode(DomainObject node) {
        var breadcrumbs = new LanguageField<List<String>>();
        var langs = node.getTranslations().stream().map(Translation::getLanguageCode).collect(Collectors.toSet());
        langs.add(Constants.DefaultLanguage);
        for (var lang : langs) {
            var crumbs = new ArrayList<String>();
            crumbs.add(node.getTranslation(lang).map(JsonTranslation::getName).orElse(node.getName()));
            breadcrumbs.put(lang, crumbs);
        }
        return breadcrumbs;
    }

    public static LanguageField<List<String>> listFromLists(LanguageField<List<String>> listLanguageField,
            LanguageField<String> languageField) {
        var breadcrumbs = SerializationUtils.clone(listLanguageField);
        languageField.keySet().forEach(key -> {
            var crumbs = breadcrumbs.computeIfAbsent(key, k -> new ArrayList<>());
            if (languageField.get(key) != null) {
                crumbs.add(languageField.get(key));
            }
        });
        return breadcrumbs;
    }
}
