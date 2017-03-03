package no.ndla.taxonomy.service.domain;

import no.ndla.taxonomy.service.repositories.CachedUrlRepository;
import no.ndla.taxonomy.service.rest.v1.UrlGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.Collection;
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
    private UrlGenerator urlGenerator;


    public UrlCacher(JdbcTemplate jdbcTemplate, CachedUrlRepository cachedUrlRepository, UrlGenerator urlGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.cachedUrlRepository = cachedUrlRepository;
        this.urlGenerator = urlGenerator;
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

    public void add(Resource resource) {
        URI publicId = resource.getPublicId();
        cachedUrlRepository.deleteByPublicId(publicId);
        Collection<String> paths = urlGenerator.generatePaths(publicId);
        String primaryPath = urlGenerator.generatePrimaryPath(publicId);

        System.out.println("primary: " + primaryPath);
        for (String path : paths) {
            System.out.println("path: " + path);
            cachedUrlRepository.save(new CachedUrl(publicId, path, path.equals(primaryPath)));
        }
    }
}
