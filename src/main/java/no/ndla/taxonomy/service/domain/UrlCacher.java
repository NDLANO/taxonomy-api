package no.ndla.taxonomy.service.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;

@Transactional
@Component
public class UrlCacher {

    private EntityManager entityManager;
    private JdbcTemplate jdbcTemplate;
    private static final String GENERATE_URLS_RECURSIVELY_QUERY = getQuery("generate_urls_recursively");

    public UrlCacher( EntityManager entityManager, JdbcTemplate jdbcTemplate) {

        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(DomainObject domainObject) {
        add(domainObject.getPublicId());
    }

    public void add(URI publicId) {
        rebuildEntireCache();
    }

    public void remove(DomainObject domainObject) {
        rebuildEntireCache();
    }

    public void remove(URI publicId) {
        rebuildEntireCache();
    }

    public void rebuildEntireCache() {
        jdbcTemplate.update("REFRESH MATERIALIZED VIEW cached_url_v;");
        jdbcTemplate.query("select count(*) from cached_url_v", resultSet -> {
            System.out.println(resultSet.getInt(1));
        });


    }
}
