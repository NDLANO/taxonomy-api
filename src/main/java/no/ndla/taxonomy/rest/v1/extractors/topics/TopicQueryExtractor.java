package no.ndla.taxonomy.rest.v1.extractors.topics;

import no.ndla.taxonomy.rest.v1.dto.topics.TopicIndexDocument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

/**
 *
 */
public class TopicQueryExtractor {

    public List<TopicIndexDocument> extractTopics(ResultSet resultSet) throws SQLException {
        List<TopicIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new TopicIndexDocument() {{
                name = resultSet.getString("topic_name");
                id = getURI(resultSet, "topic_public_id");
                contentUri = getURI(resultSet, "topic_content_uri");
                path = resultSet.getString("topic_path");
            }});
        }
        return result;
    }
}