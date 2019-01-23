package no.ndla.taxonomy.rest.v1.extractors.topics;

import no.ndla.taxonomy.rest.v1.dto.topics.SubTopicIndexDocument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

/**
 *
 */
public class SubTopicQueryExtractor {

    public List<SubTopicIndexDocument> extractSubTopics(ResultSet resultSet) throws SQLException {
        List<SubTopicIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new SubTopicIndexDocument() {{
                name = resultSet.getString("subtopic_name");
                id = getURI(resultSet, "subtopic_public_id");
                contentUri = getURI(resultSet, "subtopic_content_uri");
                isPrimary = resultSet.getBoolean("subtopic_is_primary");
            }});
        }
        return result;
    }
}
