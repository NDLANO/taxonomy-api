package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;

import java.net.URI;
import java.util.Optional;

public interface UrlResolverService {
    /**
     * @param oldUrl url previously imported into taxonomy with taxonomy-import
     * @return return a resolved URL or null
     */
    Optional<String> resolveOldUrl(String oldUrl);

    void putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption;

    Optional<ResolvedUrl> resolveUrl(String path) throws InvalidArgumentServiceException;

    class NodeIdNotFoundExeption extends Exception {
        public NodeIdNotFoundExeption(String message) {
            super(message);
        }
    }
}
