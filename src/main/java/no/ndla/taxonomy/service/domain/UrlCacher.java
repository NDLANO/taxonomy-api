package no.ndla.taxonomy.service.domain;

import no.ndla.taxonomy.service.repositories.CachedUrlRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.getURI;

@Transactional
@Component
public class UrlCacher {
    private CachedUrlRepository cachedUrlRepository;
    private EntityManager entityManager;
    private JdbcTemplate jdbcTemplate;
    private static final String GENERATE_URLS_RECURSIVELY_QUERY = getQuery("generate_urls_recursively");

    public UrlCacher(CachedUrlRepository cachedUrlRepository, EntityManager entityManager, JdbcTemplate jdbcTemplate) {
        this.cachedUrlRepository = cachedUrlRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(DomainObject domainObject) {
        //Naive implementation for now
        rebuildEntireCache();

    }

    public void remove(DomainObject domainObject) {
        //Naive implementation for now
        rebuildEntireCache();
    }

    private void rebuildEntireCache() {
        cachedUrlRepository.deleteAll();
        entityManager.flush();
        jdbcTemplate.query(GENERATE_URLS_RECURSIVELY_QUERY, resultSet -> {
            while (resultSet.next()) {
                CachedUrl url = new CachedUrl();
                url.setPublicId(getURI(resultSet, "public_id"));
                url.setPath(resultSet.getString("path"));
                url.setPrimary(resultSet.getBoolean("is_primary"));
                cachedUrlRepository.save(url);
            }
            return null;
        });
    }
}
