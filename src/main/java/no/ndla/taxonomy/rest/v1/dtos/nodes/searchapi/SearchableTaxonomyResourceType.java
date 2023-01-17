package no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ndla.taxonomy.domain.ResourceType;

import java.util.HashMap;

public class SearchableTaxonomyResourceType {
    static String DefaultLanguage = "nb";

    @JsonProperty
    private String id;

    @JsonProperty
    private HashMap<String, String> name;

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
