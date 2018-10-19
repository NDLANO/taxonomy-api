package no.ndla.taxonomy.rest.v1.extractors;

import no.ndla.taxonomy.rest.v1.dto.topics.FilterIndexDocument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

/**
 *
 */
public class FilterQueryExtractor {

    public List<FilterIndexDocument> extractFilters(ResultSet resultSet) throws SQLException {
        List<FilterIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new FilterIndexDocument() {{
                name = resultSet.getString("filter_name");
                id = getURI(resultSet, "filter_public_id");
                connectionId = getURI(resultSet, "topic_filter_public_id");
                relevanceId = getURI(resultSet, "relevance_id");
            }});
        }
        return result;
    }
}