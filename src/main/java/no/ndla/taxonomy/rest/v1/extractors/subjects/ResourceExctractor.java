package no.ndla.taxonomy.rest.v1.extractors.subjects;

import no.ndla.taxonomy.rest.v1.dto.subjects.ResourceFilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.subjects.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dto.subjects.ResourceTypeIndexDocument;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;
import static no.ndla.taxonomy.jdbc.QueryUtils.toURI;
import static no.ndla.taxonomy.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

/**
 *
 */
public class ResourceExctractor {



    public List<ResourceIndexDocument> extractResources(URI subjectId, URI relevance, ResultSet resultSet) throws SQLException {
        List<ResourceIndexDocument> result = new ArrayList<>();
        Map<URI, ResourceIndexDocument> resources = new HashMap<>();

        String context = "/" + subjectId.toString().substring(4);

        while (resultSet.next()) {
            URI id = toURI(resultSet.getString("resource_public_id"));

            ResourceIndexDocument resource = extractResource(relevance, resultSet, result, resources, id);
            resource.path = getPathMostCloselyMatchingContext(context, resource.path, resultSet.getString("resource_path"));
            extractResourceType(resultSet, resource);
            extractFilter(resultSet, resource);
        }
        return result;
    }

    private void extractFilter(ResultSet resultSet, ResourceIndexDocument resource) throws SQLException {
        URI filterPublicId = getURI(resultSet, "filter_public_id");
        if (null != filterPublicId) {
            ResourceFilterIndexDocument filter = new ResourceFilterIndexDocument() {{
                id = filterPublicId;
                relevanceId = getURI(resultSet, "relevance_public_id");
            }};
            System.out.println("Adding filter "+filterPublicId +" with relevance "+ filter.relevanceId);
            resource.filters.add(filter);
        }
    }

    private void extractResourceType(ResultSet resultSet, ResourceIndexDocument resource) throws SQLException {
        URI resource_type_id = getURI(resultSet, "resource_type_public_id");
        if (resource_type_id != null) {
            ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                id = resource_type_id;
                name = resultSet.getString("resource_type_name");
            }};

            resource.resourceTypes.add(resourceType);
        }
    }

    public ResourceIndexDocument extractResource(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, Map<URI, ResourceIndexDocument> resources, URI id) throws SQLException {
        ResourceIndexDocument resource = resources.get(id);
        if (null == resource) {
            resource = new ResourceIndexDocument() {{
                topicId = toURI(resultSet.getString("topic_public_id"));
                topicNumericId = resultSet.getInt("topic_id");
                contentUri = toURI(resultSet.getString("resource_content_uri"));
                name = resultSet.getString("resource_name");
                id = toURI(resultSet.getString("resource_public_id"));
                connectionId = toURI(resultSet.getString("connection_public_id"));
                rank = resultSet.getInt("rank");
            }};
            resources.put(id, resource);
            filterResourceByRelevance(relevance, resultSet, result, resource);
        }
        return resource;
    }

    private void filterResourceByRelevance(URI relevance, ResultSet resultSet, List<ResourceIndexDocument> result, ResourceIndexDocument resource) throws SQLException {
        if (relevance == null || relevance.toString().equals("")) {
            result.add(resource);
        } else {
            URI resourceRelevance = toURI(resultSet.getString("relevance_public_id"));
            if (resourceRelevance != null && resourceRelevance.equals(relevance)) {
                result.add(resource);
            }
        }
    }
}
