package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedUrlOldRig;
import no.ndla.taxonomy.rest.v1.UrlResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.jdbc.QueryUtils.getURI;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;
import static no.ndla.taxonomy.rest.v1.UrlResolver.RESOLVE_URL_QUERY;

@Service
public class UrlResolverService {

    JdbcTemplate jdbcTemplate;

    @Autowired
    public UrlResolverService(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * @param oldUrl url previously imported into taxonomy with taxonomy-import
     * @return return a resolved URL or null
     */
    public String resolveOldUrl(String oldUrl) {
        List<CachedUrlOldRig> results = getCachedUrlOldRig("SELECT PUBLIC_ID, SUBJECT_ID FROM CACHED_URL_OLD_RIG WHERE OLD_URL=?", asList(oldUrl));
        if (!results.isEmpty()) {
            CachedUrlOldRig result = results.get(0);
            List<String> allPaths = getAllPaths(result.getPublic_id());
            if (result.getSubject_id() != null) {
                String shortestPath = findshortestPathStartingWith(result.getSubject_id(), allPaths);
                if (shortestPath != null) {
                    return shortestPath;
                } else {
                    return getPrimaryPath(result.getPublic_id());
                }
            } else {
                return getPrimaryPath(result.getPublic_id());
            }
        } else {
            return null;
        }
    }


    private String findshortestPathStartingWith(URI subject_id, List<String> allPaths) {
        String subject = "/" + subject_id.toString().split("urn:")[1];
        allPaths.sort(Comparator.comparing(String::length));
        for (String path : allPaths) {
            if (path.startsWith(subject)) {
                return path;
            }
        }
        return null;
    }

    private List<String> getAllPaths(URI public_id) {
        List<String> allpaths = jdbcTemplate.query("SELECT PATH, IS_PRIMARY FROM CACHED_URL WHERE PUBLIC_ID=?", setQueryParameters(Collections.singletonList(public_id.toString())),
                (resultSet, rowNum) -> resultSet.getString("path")
        );
        return allpaths;
    }

    private String getPrimaryPath(URI public_id) {
        UrlResolver.ResolvedUrl returnedResolvedUrl = jdbcTemplate.query(RESOLVE_URL_QUERY, setQueryParameters(Collections.singletonList(public_id.toString())),
                resultSet -> {
                    UrlResolver.ResolvedUrl resolvedUrl = new UrlResolver.ResolvedUrl();
                    while (resultSet.next()) {
                        resolvedUrl.id = getURI(resultSet, "public_id");
                        resolvedUrl.contentUri = getURI(resultSet, "content_uri");
                        resolvedUrl.name = resultSet.getString("name");
                        resolvedUrl.path = resultSet.getString("resource_path");
                    }
                    return resolvedUrl;
                }
        );
        return "/subject:1/topic:1:183926";
    }

    private List<CachedUrlOldRig> getCachedUrlOldRig(String sql, List<Object> args) {
        final List<CachedUrlOldRig> query = jdbcTemplate.query(sql, setQueryParameters(args),
                (resultSet, rowNum) -> new CachedUrlOldRig() {{
                    setPublic_id(resultSet.getString("public_id"));
                    if (resultSet.getString("subject_id") != null) {
                        setSubject_id(resultSet.getString("subject_id"));
                    }
                }}
        );
        return query;
    }


    private class PathInfo {
        public String path;
        public Boolean is_primary;
    }
}
