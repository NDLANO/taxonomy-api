package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedUrlOldRig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@Service
/*
 This class is both a service and a database repository class
 */
public class UrlResolverService {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UrlResolverService(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * @param oldUrl url previously imported into taxonomy with taxonomy-import
     * @return return a resolved URL or null
     */
    public String resolveOldUrl(String oldUrl) {
        List<CachedUrlOldRig> results = getCachedUrlOldRig(oldUrl);
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
        return allPaths.stream().filter(path -> path.startsWith(subject)).findFirst().orElse(null);
    }

    private List<String> getAllPaths(URI public_id) {
        List<String> allpaths = jdbcTemplate.query("SELECT PATH, IS_PRIMARY FROM CACHED_URL WHERE PUBLIC_ID=?", setQueryParameters(Collections.singletonList(public_id.toString())),
                (resultSet, rowNum) -> resultSet.getString("path")
        );
        return allpaths;
    }

    private String getPrimaryPath(URI public_id) {
        List<String> primaryPaths = jdbcTemplate.query("SELECT PATH, IS_PRIMARY FROM CACHED_URL WHERE PUBLIC_ID=? AND IS_PRIMARY=TRUE", setQueryParameters(Collections.singletonList(public_id.toString())),
                (resultSet, rowNum) -> resultSet.getString("path")
        );
        if (primaryPaths.isEmpty()) return null;
        return primaryPaths.get(0);
    }

    private List<CachedUrlOldRig> getCachedUrlOldRig(String oldUrl) {
        final String sql = "SELECT PUBLIC_ID, SUBJECT_ID FROM CACHED_URL_OLD_RIG WHERE OLD_URL=?";
        final List<CachedUrlOldRig> query = jdbcTemplate.query(sql, setQueryParameters(asList(oldUrl)),
                (resultSet, rowNum) -> new CachedUrlOldRig() {{
                    setPublic_id(resultSet.getString("public_id"));
                    if (resultSet.getString("subject_id") != null) {
                        setSubject_id(resultSet.getString("subject_id"));
                    }
                }}
        );
        return query;
    }

    /**
     * put old url into CACHED_URL_OLD_RIG
     *
     * @param oldUrl    url to put
     * @param nodeId    nodeID to be associated with this URL
     * @param subjectId subjectID to be associated with this URL (optional)
     * @return true in order to be mockable "given" ugh!
     * @throws NodeIdNotFoundExeption if node ide not found in taxonomy
     */
    public Boolean putPath(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption {

        if (getAllPaths(nodeId).isEmpty())
            throw new NodeIdNotFoundExeption("Node id not found in taxonomy for " + oldUrl);
        if (getCachedUrlOldRig(oldUrl).isEmpty()) {
            String sql = "INSERT INTO CACHED_URL_OLD_RIG (OLD_URL, PUBLIC_ID, SUBJECT_ID) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, oldUrl, nodeId.toString(), subjectId.toString());
        } else {
            String sql = "UPDATE CACHED_URL_OLD_RIG SET PUBLIC_ID=?, SUBJECT_ID=? WHERE OLD_URL=?";
            jdbcTemplate.update(sql, nodeId.toString(), subjectId.toString(), oldUrl);
        }

        if (oldUrl.contains("?fag=")) {
            return putPath(oldUrl.substring(0, oldUrl.indexOf("?fag=")), nodeId, subjectId);
        }
        return true;
    }

    public static class NodeIdNotFoundExeption extends Exception {
        public NodeIdNotFoundExeption(String message) {
            super(message);
        }
    }
}
