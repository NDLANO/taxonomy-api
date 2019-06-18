package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.ResolvablePathEntity;
import no.ndla.taxonomy.domain.ResolvedPath;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.ResolvedPathRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@Service
/*
 This class is both a service and a database repository class
 */
public class UrlResolverService {

    private JdbcTemplate jdbcTemplate;
    private OldUrlCanonifier canonifier = new OldUrlCanonifier();

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private final ResolvedPathRepository resolvedPathRepository;

    @Autowired
    public UrlResolverService(DataSource dataSource,
                              SubjectRepository subjectRepository,
                              TopicRepository topicRepository,
                              ResourceRepository resourceRepository,
                              ResolvedPathRepository resolvedPathRepository) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.resourceRepository = resourceRepository;
        this.resolvedPathRepository = resolvedPathRepository;

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * @param oldUrl url previously imported into taxonomy with taxonomy-import
     * @return return a resolved URL or null
     */
    public String resolveUrl(String oldUrl) {
        List<UrlMapping> results = getCachedUrlOldRig(oldUrl);
        if (!results.isEmpty()) {
            UrlMapping result = results.get(0);
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
        return resolvedPathRepository.getAllByPublicId(public_id)
                .stream()
                .map(ResolvedPath::getPath)
                .collect(Collectors.toList());
    }

    private String getPrimaryPath(URI public_id) {
        return resolvedPathRepository.getFirstByPublicIdAndIsPrimary(public_id, true)
                .map(ResolvedPath::getPath)
                .orElse(null);
    }

    private List<UrlMapping> getCachedUrlOldRig(String oldUrl) {
        String canonicalUrl = canonifier.canonify(oldUrl);
        String queryUrl = canonicalUrl + "%";
        final String sql = "SELECT old_url, public_id, subject_id FROM URL_MAP WHERE old_url LIKE ?";
        List<UrlMapping> result = new ArrayList<>();
        jdbcTemplate.query(sql, setQueryParameters(queryUrl), (RowCallbackHandler) resultSet -> {
            String matchedUrl = resultSet.getString("old_url");
            //the LIKE query may match node IDs that __start with__ the same node ID as in old url
            //e.g. oldUrl /node/54 should not match /node/54321 - therefore we add only if IDs match
            if (getNodeId(canonicalUrl).equals(getNodeId(matchedUrl))) {
                result.add(new UrlMapping() {{
                    setPublic_id(resultSet.getString("public_id"));
                    if (resultSet.getString("subject_id") != null) {
                        setSubject_id(resultSet.getString("subject_id"));
                    }
                }});
            }
        });
        return result;
    }

    private String getNodeId(String url) {
        if (url != null) {
            if (url.contains("?") && url.contains("/")) {
                return url.substring(url.lastIndexOf("/"), url.indexOf("?"));
            } else if (url.contains("/")) {
                return url.substring(url.lastIndexOf("/"));
            }
        }
        return null;
    }

    /**
     * put old url into URL_MAP
     *
     * @param oldUrl    url to put
     * @param nodeId    nodeID to be associated with this URL
     * @param subjectId subjectID to be associated with this URL (optional)
     * @return true in order to be mockable "given" ugh!
     * @throws NodeIdNotFoundExeption if node ide not found in taxonomy
     */
    public Boolean putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption {
        oldUrl = canonifier.canonify(oldUrl);
        if (getAllPaths(nodeId).isEmpty())
            throw new NodeIdNotFoundExeption("Node id not found in taxonomy for " + oldUrl);
        if (getCachedUrlOldRig(oldUrl).isEmpty()) {
            String sql = "INSERT INTO URL_MAP (OLD_URL, PUBLIC_ID, SUBJECT_ID) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, oldUrl, nodeId.toString(), subjectId.toString());
        } else {
            String sql = "UPDATE URL_MAP SET PUBLIC_ID=?, SUBJECT_ID=? WHERE OLD_URL=?";
            jdbcTemplate.update(sql, nodeId.toString(), subjectId.toString(), oldUrl);
        }
        return true;
    }

    public static class NodeIdNotFoundExeption extends Exception {
        public NodeIdNotFoundExeption(String message) {
            super(message);
        }
    }

    @Transactional(readOnly = true)
    public Set<ResolvablePathEntity> getResolvablePathEntitiesFromPublicId(URI publicId) {
        final var entries = new HashSet<ResolvablePathEntity>();

        entries.addAll(subjectRepository.findAllByPublicIdIncludingResolvedPaths(publicId));
        entries.addAll(topicRepository.findAllByPublicIdIncludingResolvedPaths(publicId));
        entries.addAll(resourceRepository.findAllByPublicIdIncludingResolvedPaths(publicId));

        return entries;
    }
}
