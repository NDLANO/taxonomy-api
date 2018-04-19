package no.ndla.taxonomy.service.domain;

import no.ndla.taxonomy.service.repositories.CachedUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.getURI;

@Transactional
@Component
public class UrlCacher {
    private CachedUrlRepository cachedUrlRepository;
    private EntityManager entityManager;
    private JdbcTemplate jdbcTemplate;
    private static final String GENERATE_URLS_RECURSIVELY_QUERY = getQuery("generate_urls_recursively");

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlCacher.class);

    public UrlCacher(CachedUrlRepository cachedUrlRepository, EntityManager entityManager, JdbcTemplate jdbcTemplate) {
        this.cachedUrlRepository = cachedUrlRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(DomainObject domainObject) {
        add(domainObject.getPublicId());
    }

    public void add(URI publicId) {
        //Naive implementation for now
        rebuildEntireCache();
    }

    public void remove(DomainObject domainObject) {
        remove(domainObject.getPublicId());
    }

    public void remove(URI publicId) {

        //Naive implementation for now
        rebuildEntireCache();
    }

    public void rebuildEntireCache() {
        jdbcTemplate.update("TRUNCATE CACHED_URL;");
        long startTime = System.currentTimeMillis();
        List<Object[]> urls = new ArrayList<>();
        jdbcTemplate.query(GENERATE_URLS_RECURSIVELY_QUERY, resultSet -> {
            int count = 0;
            while (resultSet.next()) {
                count++;
                Object[] url = new Object[3];
                url[0] = getURI(resultSet, "public_id").toASCIIString();
                url[1] = (resultSet.getString("path"));
                url[2] = (resultSet.getBoolean("is_primary"));
                urls.add(url);
            }
            LOGGER.info("URL cache rebuilt for " + count + " entities");
        });
        jdbcTemplate.batchUpdate("INSERT INTO CACHED_URL(PUBLIC_ID, PATH, IS_PRIMARY) VALUES(?,?,?)", urls);
        long endTime = System.currentTimeMillis();
        LOGGER.info("Time to update URL cache: " + (endTime - startTime) + " ms");
    }
}
