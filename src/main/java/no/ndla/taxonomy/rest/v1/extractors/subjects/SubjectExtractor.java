package no.ndla.taxonomy.rest.v1.extractors.subjects;

import no.ndla.taxonomy.rest.v1.dto.subjects.SubjectIndexDocument;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;

/**
 *
 */
public class SubjectExtractor {
    public List<SubjectIndexDocument> extractSubjects(ResultSet resultSet) throws SQLException {
        List<SubjectIndexDocument> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(new SubjectIndexDocument() {{
                name = resultSet.getString("subject_name");
                id = getURI(resultSet, "subject_public_id");
                contentUri = getURI(resultSet, "subject_content_uri");
                path = resultSet.getString("subject_path");
            }});
        }
        return result;
    }
}
