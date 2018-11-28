package no.ndla.taxonomy.rest.v1.extractors;

import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.rest.v1.dto.topics.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.topics.ResourceTypeIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.topics.TopicIndexDocument;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.jdbc.QueryUtils.toURI;
import static no.ndla.taxonomy.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

/**
 *
 */
public class ResourceQueryExtractor {

    public String addFiltersToQuery(URI[] filterIds, List<Object> args, String query) {
        if (filterIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI filterId : filterIds) {
                where.append("f.public_id = ? OR ");
                args.add(filterId.toString());
            }
            where.setLength(where.length() - 4);
            query = query.replace("2 = 2", "(" + where + ")");
        }
        return query;
    }

    public String addResourceTypesToQuery(URI[] resourceTypeIds, List<Object> args, String query) {
        if (resourceTypeIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI resourceTypeId : resourceTypeIds) {
                where.append("rt.public_id = ? OR ");
                args.add(resourceTypeId.toString());
            }
            where.setLength(where.length() - 4);
            query = query.replace("1 = 1", "(" + where + ")");
        }
        return query;
    }

    public List<ResourceIndexDocument> extractResources(@RequestParam(value = "relevance", required = false, defaultValue = "") @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.") URI relevance, TopicIndexDocument topicIndexDocument, ResultSet resultSet) throws SQLException {
        List<ResourceIndexDocument> result = new ArrayList<>();
        Map<URI, ResourceIndexDocument> resources = new HashMap<>();

        String context = topicIndexDocument.path;

        while (resultSet.next()) {
            URI id = toURI(resultSet.getString("resource_public_id"));

            ResourceIndexDocument resource = resources.get(id);
            if (null == resource) {
                resource = new ResourceIndexDocument() {{
                    topicId = toURI(resultSet.getString("topic_id"));
                    topicNumericId = resultSet.getInt("topic_numeric_id");
                    name = resultSet.getString("resource_name");
                    contentUri = toURI(resultSet.getString("resource_content_uri"));
                    id = toURI(resultSet.getString("resource_public_id"));
                    connectionId = toURI(resultSet.getString("connection_public_id"));
                    rank = resultSet.getInt("rank");
                    isPrimary = resultSet.getBoolean("resource_is_primary");
                }};
                resources.put(id, resource);
            }
            filterResultByRelevance(relevance, resultSet, result, resource);
            resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));

            String resourceTypePublicId = resultSet.getString("resource_type_public_id");
            if (resourceTypePublicId != null) {
                ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                    id = toURI(resourceTypePublicId);
                    name = resultSet.getString("resource_type_name");
                }};

                resource.resourceTypes.add(resourceType);
            }
        }

        return result;
    }

    private void filterResultByRelevance(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, ResourceIndexDocument resource) throws SQLException {
        if (relevance == null || relevance.toString().equals("") && !result.contains(resource)) {
                result.add(resource);
        } else {
            URI resourceRelevance = toURI(resultSet.getString("relevance_public_id"));
            if (resourceRelevance != null && resourceRelevance.equals(relevance) && !result.contains(resource)) {
                result.add(resource);
            }
        }
    }
}
