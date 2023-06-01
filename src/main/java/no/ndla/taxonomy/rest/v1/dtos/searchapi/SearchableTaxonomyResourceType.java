/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.searchapi;

import static no.ndla.taxonomy.config.Constants.DefaultLanguage;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import no.ndla.taxonomy.domain.ResourceType;

public class SearchableTaxonomyResourceType {
    @JsonProperty
    private String id;

    @JsonProperty
    private HashMap<String, String> name;

    public SearchableTaxonomyResourceType() {}

    public SearchableTaxonomyResourceType(ResourceType resourceType) {
        var translations = resourceType.getTranslations();
        this.id = resourceType.getPublicId().toString();
        this.name = new HashMap<>();
        this.name.put(DefaultLanguage, resourceType.getName());

        for (var t : translations) {
            name.put(t.getLanguageCode(), t.getName());
        }
    }
}
