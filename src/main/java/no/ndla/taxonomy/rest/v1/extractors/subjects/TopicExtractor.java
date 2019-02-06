package no.ndla.taxonomy.rest.v1.extractors.subjects;

import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.TopicFilterIndexDocument;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;
import static no.ndla.taxonomy.jdbc.QueryUtils.toURI;
import static no.ndla.taxonomy.rest.v1.UrlResolver.getPathMostCloselyMatchingContext;

/**
 *
 */
public class TopicExtractor {
    public List<SubTopicIndexDocument> extractTopics(URI id, URI[] filterIds, URI relevance, ResultSet resultSet) throws SQLException {
        Map<URI, SubTopicIndexDocument> topics = new HashMap<>();
        List<SubTopicIndexDocument> queryresult = new ArrayList<>();
        String context = "/" + id.toString().substring(4);
        while (resultSet.next()) {
            URI public_id = getURI(resultSet, "public_id");

            SubTopicIndexDocument topic = extractTopic(relevance, resultSet, topics, queryresult, public_id);
            topic.path = getPathMostCloselyMatchingContext(context, topic.path, resultSet.getString("topic_path"));
            extractFilter(resultSet, topic);
        }
        return filterTopics(filterIds, topics, queryresult);
    }

    private void extractFilter(ResultSet resultSet, SubTopicIndexDocument topic) throws SQLException {
        URI filterPublicId = getURI(resultSet, "filter_public_id");
        if (null != filterPublicId) {
            TopicFilterIndexDocument filter = new TopicFilterIndexDocument() {{
                id = filterPublicId;
                name = resultSet.getString("filter_name");
                relevanceId = getURI(resultSet, "relevance_public_id");
            }};

            topic.filters.add(filter);
        }
    }

    private List<SubTopicIndexDocument> filterTopics(URI[] filterIds, Map<URI, SubTopicIndexDocument> topics, List<SubTopicIndexDocument> queryresult) {
        if (filterIds != null && filterIds.length > 0) {
            Set<SubTopicIndexDocument> result = new HashSet<>();
            List<URI> filtersInQuery = asList(filterIds);
            for (SubTopicIndexDocument doc : queryresult) {
                for (TopicFilterIndexDocument filterInDoc : doc.filters) {
                    if (filtersInQuery.contains(filterInDoc.id)) {
                        result.add(doc);
                        SubTopicIndexDocument current = doc;
                        while ((current = topics.get(current.parent)) != null) {
                            result.add(current);
                        }
                    }
                }
            }
            return new ArrayList<>(result);
        } else {
            return queryresult;
        }
    }

    private SubTopicIndexDocument extractTopic(URI relevance, ResultSet resultSet, Map<URI, SubTopicIndexDocument> topics, List<SubTopicIndexDocument> queryresult, URI public_id) throws SQLException {
        SubTopicIndexDocument topic = topics.get(public_id);
        if (topic == null) {
            topic = new SubTopicIndexDocument() {{
                name = resultSet.getString("name");
                id = public_id;
                contentUri = getURI(resultSet, "content_uri");
                parent = getURI(resultSet, "parent_public_id");
                connectionId = getURI(resultSet, "connection_public_id");
                topicFilterId = getURI(resultSet, "topic_filter_public_id");
                filterPublicId = getURI(resultSet, "filter_public_id");
                isPrimary = resultSet.getBoolean("is_primary");
                rank = resultSet.getInt("rank");
            }};
            topics.put(topic.id, topic);
            filterTopicByRelevance(relevance, resultSet, queryresult, topic);
        }
        return topic;
    }

    private void filterTopicByRelevance(URI relevance, ResultSet resultSet, List<SubTopicIndexDocument> queryresult, SubTopicIndexDocument topic) throws SQLException {
        if (relevance == null || relevance.toString().equals("")) {
            queryresult.add(topic);
        } else {
            URI topicRelevance = toURI(resultSet.getString("relevance_public_id"));
            if (topicRelevance != null && topicRelevance.equals(relevance)) {
                queryresult.add(topic);
            }
        }
    }
}
