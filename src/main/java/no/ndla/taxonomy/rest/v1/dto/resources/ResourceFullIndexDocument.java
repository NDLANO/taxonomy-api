package no.ndla.taxonomy.rest.v1.dto.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@ApiModel("ResourceFullIndexDocument")
public class ResourceFullIndexDocument extends ResourceIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Related resource type(s)", example = "[" +
            "{\"id\": \"urn:resourcetype:learningPath\"," +
            " \"name\": \"Læringssti\"}]")
    public Set<ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "Filters", example = "[" +
            "{\"id\": \"urn:filter:047bb226-48d1-4122-8791-7d4f5d83cf8b\"," +
            "\"name\": \"VG2\"," +
            "\"connectionId\": \"urn:resource-filter:a41d6162-b67f-44d8-b440-c1fdc7b4d05e\"," +
            "\"relevanceId\": \"urn:relevance:core\"}]")
    public Set<FilterIndexDocument> filters = new HashSet<>();

    @JsonProperty
    @ApiModelProperty(value = "Parent topology nodes and whether or not connection type is primary",
            example = "[" +
                    "{\"id\": \"urn:topic:1:181900\"," +
                    "\"name\": \"I dyrehagen\"," +
                    "\"contentUri\": \"urn:article:6662\"," +
                    "\"path\": \"/subject:2/topic:1:181900\"," +
                    "\"primary\": \"true\"}]")
    public Set<ParentTopicIndexDocument> parentTopics = new HashSet<>();

    public List<String> paths = new ArrayList<>();

    public static ResourceFullIndexDocument from(ResourceIndexDocument resource) {
        ResourceFullIndexDocument r = new ResourceFullIndexDocument();
        r.id = resource.id;
        r.name = resource.name;
        r.contentUri = resource.contentUri;
        r.path = resource.path;
        return r;
    }
}
