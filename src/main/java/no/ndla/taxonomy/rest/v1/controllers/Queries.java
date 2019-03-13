package no.ndla.taxonomy.rest.v1.controllers;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.rest.v1.dtos.queries.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.queries.ResourceTypeIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.queries.TopicIndexDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;

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
    public List<ResourceIndexDocument> queryResources(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return jdbcTemplate.query(GET_RESOURCES_BY_CONTENT_URI_QUERY,
                setQueryParameters(language, language, contentURI.toString()),
                this::extractResources);
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found.")
    public List<TopicIndexDocument> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return jdbcTemplate.query(GET_TOPICS_BY_CONTENT_URI_QUERY,
                setQueryParameters(language, contentURI.toString()),
                this::extractTopics);
    }

    private List<TopicIndexDocument> extractTopics(ResultSet resultSet) throws SQLException {
        Map<URI, TopicIndexDocument> topics = new HashMap<>();
        while (resultSet.next()) {
            URI id = toURI(resultSet.getString("topic_public_id"));
            String path = resultSet.getString("topic_path");
            boolean primary = resultSet.getBoolean("path_is_primary");

            TopicIndexDocument topic = topics.get(id);
            if (null == topic) {
                topic = new TopicIndexDocument();
                topic.name = resultSet.getString("topic_name");
                topic.contentUri = getURI(resultSet, "topic_content_uri");
                topic.id = id;
                topic.paths = new HashSet<>();
                topics.put(id, topic);
            }
            if (primary && (topic.path == null || !topic.path.startsWith("/topic"))) {
                topic.path = path;
            }
            topic.paths.add(path);
        }
        return new ArrayList<>(topics.values());
    }

    private List<ResourceIndexDocument> extractResources(ResultSet resultSet) throws SQLException {
        Map<URI, ResourceIndexDocument> resources = new HashMap<>();

        while (resultSet.next()) {
            URI id = toURI(resultSet.getString("resource_public_id"));
            String path = resultSet.getString("resource_path");
            boolean primary = resultSet.getBoolean("path_is_primary");

            ResourceIndexDocument resource = resources.get(id);
            if (null == resource) {
                resource = new ResourceIndexDocument();
                resource.name = resultSet.getString("resource_name");
                resource.contentUri = toURI(resultSet.getString("resource_content_uri"));
                resource.id = id;
                resource.paths = new HashSet<>();
                resource.resourceTypes = new HashSet<>();
                resources.put(id, resource);
            }
            String resourceTypePublicId = resultSet.getString("resource_type_public_id");
            if (resourceTypePublicId != null) {
                ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                    id = toURI(resourceTypePublicId);
                    name = resultSet.getString("resource_type_name");
                }};
                resource.resourceTypes.add(resourceType);
            }
            if (primary && (resource.path == null || !resource.path.startsWith("/topic"))) {
                resource.path = path;
            }
            resource.paths.add(path);

        }
        return new ArrayList<>(resources.values());
    }


}
