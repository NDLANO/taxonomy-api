package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedUrlOldRig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@Service
public class UrlResolverService {

    JdbcTemplate jdbcTemplate;

    @Autowired
    public UrlResolverService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String resolveOldUrl(String oldUrl) {
        List<CachedUrlOldRig> results = getResourceIndexDocuments("SELECT PUBLIC_ID, SUBJECT_ID FROM CACHED_URL_OLD_RIG WHERE OLD_URL=?", asList(oldUrl));
        if (!results.isEmpty()) {
            return ("test");
        } else {
            return null;
        }
    }

    private List<CachedUrlOldRig> getResourceIndexDocuments(String sql, List<Object> args) {
        final List<CachedUrlOldRig> query = jdbcTemplate.query(sql, setQueryParameters(args),
                (resultSet, rowNum) -> new CachedUrlOldRig() {{
                    setPublic_id(resultSet.getString("public_id"));
                    setSubject_id(resultSet.getString("subject_id"));
                }}
        );
        return query;
    }


}
