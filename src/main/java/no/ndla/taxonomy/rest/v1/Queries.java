package no.ndla.taxonomy.rest.v1;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.rest.v1.dto.queries.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.queries.ResourceTypeIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.queries.TopicIndexDocument;
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
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

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
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {

        return jdbcTemplate.query(GET_RESOURCES_BY_CONTENT_URI_QUERY, setQueryParameters(language, language, contentURI.toString()),
                this::extractResources);
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found.")
    public List<TopicIndexDocument> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "context", required = false, defaultValue = "") String context
    ) throws Exception {

        return jdbcTemplate.query(GET_TOPICS_BY_CONTENT_URI_QUERY, setQueryParameters(language, contentURI.toString()),
                (resultSet, rowNum) -> new TopicIndexDocument() {{
                    name = resultSet.getString("topic_name");
                    id = getURI(resultSet, "topic_public_id");
                    contentUri = getURI(resultSet, "topic_content_uri");
                    path = resultSet.getString("topic_path");
                }});
    }

    private List<ResourceIndexDocument> extractResources(ResultSet resultSet) throws SQLException {
        Map<URI, ResourceIndexDocument> resources = new HashMap<>();

        while (resultSet.next()) {
            URI id = toURI(resultSet.getString("resource_public_id"));
            String path = resultSet.getString("resource_path");
            boolean primary = resultSet.getBoolean("path_is_primary");

            ResourceIndexDocument resource = resources.get(id);
            if (null == resource) {
                resource = new ResourceIndexDocument() {{
                    name = resultSet.getString("resource_name");
                    contentUri = toURI(resultSet.getString("resource_content_uri"));
                    id = toURI(resultSet.getString("resource_public_id"));
                    paths = new HashSet<>();
                    resourceTypes = new HashSet<>();
                }};
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
