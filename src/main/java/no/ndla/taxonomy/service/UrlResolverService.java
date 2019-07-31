package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.CachedUrl;
import no.ndla.taxonomy.domain.CachedUrlEntity;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
/*
 This class is both a service and a database repository class
 */
public class UrlResolverService {

    private final OldUrlCanonifier canonifier = new OldUrlCanonifier();

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private final CachedUrlRepository cachedUrlRepository;
    private final UrlMappingRepository urlMappingRepository;

    @Autowired
    public UrlResolverService(SubjectRepository subjectRepository,
                              TopicRepository topicRepository,
                              ResourceRepository resourceRepository,
                              CachedUrlRepository cachedUrlRepository,
                              UrlMappingRepository urlMappingRepository) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.resourceRepository = resourceRepository;
        this.cachedUrlRepository = cachedUrlRepository;
        this.urlMappingRepository = urlMappingRepository;
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
        return cachedUrlRepository.findAllByPublicId(public_id)
                .stream()
                .map(CachedUrl::getPath)
                .collect(Collectors.toList());
    }

    private String getPrimaryPath(URI public_id) {
        return cachedUrlRepository.findFirstByPublicIdAndPrimary(public_id, true)
                .map(CachedUrl::getPath)
                .orElse(null);
    }

    private List<UrlMapping> getCachedUrlOldRig(String oldUrl) {
        String canonicalUrl = canonifier.canonify(oldUrl);
        String queryUrl = canonicalUrl + "%";

        return urlMappingRepository.findAllByOldUrlLike(queryUrl)
                .stream()
                //the LIKE query may match node IDs that __start with__ the same node ID as in old url
                //e.g. oldUrl /node/54 should not match /node/54321 - therefore we add only if IDs match
                .filter(urlMapping -> urlMapping.getOldUrl() != null)
                .filter(mapping -> getNodeId(canonicalUrl).equals(getNodeId(mapping.getOldUrl())))
                .collect(Collectors.toList());
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
    @Transactional
    public Boolean putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption {
        oldUrl = canonifier.canonify(oldUrl);
        if (getAllPaths(nodeId).isEmpty())
            throw new NodeIdNotFoundExeption("Node id not found in taxonomy for " + oldUrl);
        if (getCachedUrlOldRig(oldUrl).isEmpty()) {
            final var urlMapping = new UrlMapping();
            urlMapping.setOldUrl(oldUrl);
            urlMapping.setPublic_id(nodeId.toString());
            urlMapping.setSubject_id(subjectId);
            urlMappingRepository.save(urlMapping);
        } else {
            urlMappingRepository.findAllByOldUrl(oldUrl)
                    .forEach(urlMapping -> {
                        urlMapping.setPublic_id(nodeId.toString());
                        urlMapping.setSubject_id(subjectId.toString());
                        urlMappingRepository.save(urlMapping);
                    });
        }
        return true;
    }

    public static class NodeIdNotFoundExeption extends Exception {
        public NodeIdNotFoundExeption(String message) {
            super(message);
        }
    }

    public Set<CachedUrlEntity> getResolvablePathEntitiesFromPublicId(URI publicId) {
        final var entries = new HashSet<CachedUrlEntity>();

        entries.addAll(subjectRepository.findAllByPublicIdIncludingCachedUrls(publicId));
        entries.addAll(topicRepository.findAllByPublicIdIncludingCachedUrls(publicId));
        entries.addAll(resourceRepository.findAllByPublicIdIncludingCachedUrls(publicId));

        return entries;
    }
}
