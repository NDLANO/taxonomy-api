package no.ndla.taxonomy.service.domain;

import no.ndla.taxonomy.service.repositories.CachedUrlRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.setQueryParameters;

@Transactional
@Component
public class UrlCacher {

    private static final String GET_URLS_QUERY = getQuery("get_url_from_cache");

    private JdbcTemplate jdbcTemplate;
    private CachedUrlRepository cachedUrlRepository;


    public UrlCacher(JdbcTemplate jdbcTemplate, CachedUrlRepository cachedUrlRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.cachedUrlRepository = cachedUrlRepository;
    }

    public String getUrl(URI subjectId) {
        List<Object> args = asList(subjectId.toString());


        return jdbcTemplate.query(GET_URLS_QUERY, setQueryParameters(args),
                resultSet -> {
                    while (resultSet.next()) {
                        return resultSet.getString("path");
                    }
                    throw new NotFoundException("Subject", subjectId);

                });
    }

    public void add(String publicId) {

    }
}
