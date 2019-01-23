package no.ndla.taxonomy.rest.v1.extractors.topics;

import no.ndla.taxonomy.rest.v1.dto.topics.TopicWithPathsIndexDocument;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

public class TopicWithAllPathsQueryExtractor {

    public TopicWithPathsIndexDocument extractTopic(ResultSet resultSet) throws SQLException {
        TopicWithPathsIndexDocument doc = null;
        while (resultSet.next()) {
            String id = resultSet.getString("topic_public_id");
            String path = resultSet.getString("topic_path");
            boolean primary = resultSet.getBoolean("path_is_primary");

            if (doc == null) {
                doc = new TopicWithPathsIndexDocument();
                doc.id = URI.create(id);
                doc.name = resultSet.getString("topic_name");
                doc.contentUri = getURI(resultSet, "topic_content_uri");
                doc.paths = new ArrayList<>();
            }
            if (primary && (doc.path == null || !doc.path.startsWith("/topic"))) {
                doc.path = path;
            }
            doc.paths.add(path);

        }
        return doc;
    }
}