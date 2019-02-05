package no.ndla.taxonomy.rest.v1.extractors;

import no.ndla.taxonomy.rest.v1.dto.topics.ConnectionIndexDocument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

/**
 *
 */
public class ConnectionQueryExtractor {
    public List<ConnectionIndexDocument> extractConnections(ResultSet resultSet) throws SQLException {
        HashMap<String, ConnectionIndexDocument> resultsByConnectionId = new HashMap<>();
        while (resultSet.next()) {
            String connectionId = resultSet.getString("connection_id");
            ConnectionIndexDocument doc = resultsByConnectionId.get(connectionId);
            if (doc == null) {
                doc = new ConnectionIndexDocument();
                resultsByConnectionId.put(connectionId, doc);
            }
            doc.connectionId = getURI(resultSet, "connection_id");
            doc.isPrimary = resultSet.getBoolean("is_primary");
            doc.targetId = getURI(resultSet, "target_id");
            doc.type = resultSet.getString("connection_type");
            if (doc.isPrimary) {
                doc.paths.add(0, resultSet.getString("path"));
            } else {
                doc.paths.add(resultSet.getString("path"));
            }
        }
        return new ArrayList<>(resultsByConnectionId.values());
    }

}