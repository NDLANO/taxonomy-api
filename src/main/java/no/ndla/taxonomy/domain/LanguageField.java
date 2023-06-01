/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.config.Constants;
import org.apache.commons.lang3.SerializationUtils;

public class LanguageField<V> extends HashMap<String, V> {
    public LanguageField() {}

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
        var langs = node.getTranslations().stream()
                .map(Translation::getLanguageCode)
                .collect(Collectors.toSet());
        langs.add(Constants.DefaultLanguage);
        for (var lang : langs) {
            var crumbs = new ArrayList<String>();
            crumbs.add(node.getTranslation(lang).map(JsonTranslation::getName).orElse(node.getName()));
            breadcrumbs.put(lang, crumbs);
        }
        return breadcrumbs;
    }

    /**
     * Returns accumulated set of language fields. All additional languages from languageField are appended so all
     * languageLists have the same number of elements. If a language variant is not present, default value is used.
     */
    public static LanguageField<List<String>> listFromLists(
            LanguageField<List<String>> listLanguageField, LanguageField<String> languageField) {
        var breadcrumbs = SerializationUtils.clone(listLanguageField);
        var languages = Sets.union(listLanguageField.keySet(), languageField.keySet());
        var defaultValue = languageField.get(Constants.DefaultLanguage);
        languages.forEach(lang -> {
            var crumbs = breadcrumbs.computeIfAbsent(
                    lang, k -> listLanguageField.getOrDefault(Constants.DefaultLanguage, new ArrayList<>()));
            crumbs.add(languageField.getOrDefault(lang, defaultValue));
        });
        return breadcrumbs;
    }
}
