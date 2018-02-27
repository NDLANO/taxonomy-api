package no.ndla.taxonomy.service.rest.v1;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;
import static no.ndla.taxonomy.service.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

@RestController
@RequestMapping(path = {"/v1/queries"})
public class Queries {

    private final JdbcTemplate jdbcTemplate;

    private static final String GET_TOPICS_BY_CONTENT_URI_QUERY = getQuery("get_topics_by_contentURI");
    private static final String GET_RESOURCES_BY_CONTENT_URI_QUERY = getQuery("get_resources_by_contentURI");

    public Queries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/resources")
    @ApiOperation(value = "Gets a list of resources matching given contentURI, empty list of no matches are found.")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<Queries.ResourceIndexDocument> queryResources(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "context", required = false, defaultValue = "") String context
    ) throws Exception {

        List<Object> args = new ArrayList<>();

        args.add(language);
        args.add(language);
        args.add(contentURI.toString());
        String sql = GET_RESOURCES_BY_CONTENT_URI_QUERY.replace("1 = 1", "r.content_uri = ?");

        ResourceTypeQueryExtractor extractor = new ResourceTypeQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args), resultSet -> {
            return extractor.extractResources(context, resultSet);
        });
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found.")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<Queries.TopicIndexDocument> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "context", required = false, defaultValue = "") String context
    ) throws Exception {

        List<Object> args = new ArrayList<>();

        args.add(language);
        args.add(contentURI.toString());
        String sql = GET_TOPICS_BY_CONTENT_URI_QUERY.replace("1 = 1", "t.content_uri = ?");
        return jdbcTemplate.query(sql, setQueryParameters(args),
                (resultSet, rowNum) -> new Queries.TopicIndexDocument() {{
                    name = resultSet.getString("topic_name");
                    id = getURI(resultSet, "topic_public_id");
                    contentUri = getURI(resultSet, "topic_content_uri");
                    path = resultSet.getString("topic_path");
                }});
    }


    @ApiModel("QueryResourceIndexDocument")
    public static class ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource name", example = "Basic physics")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resourcetype:learningPath', name = 'Learning path'}]")
        public Set<Queries.ResourceTypeIndexDocument> resourceTypes = new HashSet<>();

        @JsonProperty
        @ApiModelProperty(value = "The ID of this resource in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.",
                example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
        public String path;
    }

    @ApiModel("QueryResourceTypeIndexDocument")
    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource type id", example = "urn:resourcetype:learningPath")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource type name", example = "Learning path")
        public String name;

        @Override
        @JsonIgnore
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Topics.ResourceTypeIndexDocument)) return false;

            Queries.ResourceTypeIndexDocument that = (Queries.ResourceTypeIndexDocument) o;

            return id.equals(that.id);
        }

        @Override
        @JsonIgnore
        public int hashCode() {
            return id.hashCode();
        }
    }

    private class ResourceTypeQueryExtractor {

        List<Queries.ResourceIndexDocument> extractResources(String context, ResultSet resultSet) throws SQLException {
            List<Queries.ResourceIndexDocument> result = new ArrayList<>();
            Map<URI, Queries.ResourceIndexDocument> resources = new HashMap<>();


            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

                ResourceIndexDocument resource = resources.get(id);
                if (null == resource) {
                    resource = new Queries.ResourceIndexDocument() {{
                        name = resultSet.getString("resource_name");
                        contentUri = toURI(resultSet.getString("resource_content_uri"));
                        id = toURI(resultSet.getString("resource_public_id"));
                    }};
                    resources.put(id, resource);
                }
                resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));

                String resourceTypePublicId = resultSet.getString("resource_type_public_id");
                if (resourceTypePublicId != null) {
                    Queries.ResourceTypeIndexDocument resourceType = new Queries.ResourceTypeIndexDocument() {{
                        id = toURI(resourceTypePublicId);
                        name = resultSet.getString("resource_type_name");
                    }};

                    resource.resourceTypes.add(resourceType);
                }
            }

            for (URI uri : resources.keySet()) {
                result.add(resources.get(uri));
            }
            return result;
        }
    }

    @ApiModel("QueryTopicIndexDocument")
    public static class TopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the topic", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "The path part of the url for this topic", example = "/subject:1/topic:1")
        public String path;

        TopicIndexDocument() {
        }
    }
}
