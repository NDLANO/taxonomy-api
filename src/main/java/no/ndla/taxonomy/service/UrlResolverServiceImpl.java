/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.UrlMappingRepository;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UrlResolverServiceImpl implements UrlResolverService {

    private final OldUrlCanonifier canonifier;

    private final NodeRepository nodeRepository;
    private final UrlMappingRepository urlMappingRepository;

    @Autowired
    public UrlResolverServiceImpl(UrlMappingRepository urlMappingRepository, NodeRepository nodeRepository,
            OldUrlCanonifier oldUrlCanonifier) {
        this.nodeRepository = nodeRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.canonifier = oldUrlCanonifier;
    }

    @Override
    public Optional<String> resolveOldUrl(String oldUrl) {
        final var results = getCachedUrlOldRig(oldUrl);
        if (!results.isEmpty()) {
            final var result = results.get(0);
            final var allPaths = getAllPaths(result.getPublic_id());

            if (result.getSubject_id() != null) {
                final var shortestPath = findshortestPathStartingWith(result.getSubject_id(), allPaths);

                if (shortestPath.isPresent()) {
                    return shortestPath;
                }
            }

            return getPrimaryPath(result.getPublic_id());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> findshortestPathStartingWith(URI subject_id, List<String> allPaths) {
        String subject = "/" + subject_id.toString().split("urn:")[1];
        allPaths.sort(Comparator.comparing(String::length));
        return allPaths.stream().filter(path -> path.startsWith(subject)).findFirst();
    }

    private List<String> getAllPaths(URI publicId) {
        try {
            return getEntityFromPublicId(publicId).stream().flatMap(x -> x.getAllPaths().stream())
                    .collect(Collectors.toList());
        } catch (InvalidArgumentServiceException e) {
            return List.of();
        }
    }

    private Optional<String> getPrimaryPath(URI publicId) {
        try {
            return getEntityFromPublicId(publicId).stream().map(Node::getPrimaryPath).filter(Optional::isPresent)
                    .map(Optional::get).findFirst();
        } catch (InvalidArgumentServiceException e) {
            return Optional.empty();
        }
    }

    private List<UrlMapping> getCachedUrlOldRig(String oldUrl) {
        String canonicalUrl = canonifier.canonify(oldUrl);
        String queryUrl = canonicalUrl + "%";

        return urlMappingRepository.findAllByOldUrlLike(queryUrl).stream()
                // the LIKE query may match node IDs that __start with__ the same node ID as in old
                // url
                // e.g. oldUrl /node/54 should not match /node/54321 - therefore we add only if IDs
                // match
                .filter(urlMapping -> urlMapping.getOldUrl() != null)
                .filter(mapping -> getNodeId(canonicalUrl).equals(getNodeId(mapping.getOldUrl())))
                .collect(Collectors.toList());
    }

    private Optional<String> getNodeId(String url) {
        if (url != null) {
            if (url.contains("?") && url.contains("/")) {
                return Optional.of(url.substring(url.lastIndexOf("/"), url.indexOf("?")));
            } else if (url.contains("/")) {
                return Optional.of(url.substring(url.lastIndexOf("/")));
            }
        }
        return Optional.empty();
    }

    /**
     * put old url into URL_MAP
     *
     * @param oldUrl
     *            url to put
     * @param nodeId
     *            nodeID to be associated with this URL
     * @param subjectId
     *            subjectID to be associated with this URL (optional)
     *
     * @return true in order to be mockable "given" ugh!
     *
     * @throws NodeIdNotFoundExeption
     *             if node ide not found in taxonomy
     */
    @Transactional
    @Override
    public void putUrlMapping(String oldUrl, URI nodeId, URI subjectId) throws NodeIdNotFoundExeption {
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
            urlMappingRepository.findAllByOldUrl(oldUrl).forEach(urlMapping -> {
                urlMapping.setPublic_id(nodeId.toString());
                urlMapping.setSubject_id(subjectId.toString());
                urlMappingRepository.save(urlMapping);
            });
        }
    }

    private void validateEntityPath(List<Node> entities) {
        // Verify that the path is actually valid, not just that the objects referred to exists. We
        // could use the CachedUrl
        // to do this validation, but would rather not since the CachedUrl view generation could be
        // removed later

        // Searches the array, check that the child element has the previous entity as parent
        final var lastEntity = new AtomicReference<Node>();
        for (final var entity : entities) {
            if (lastEntity.get() != null) {
                if (entity.getParentConnections().stream().noneMatch(parentConnection -> parentConnection.getParent()
                        .filter(parent -> parent.equals(lastEntity.get())).isPresent())) {
                    throw new NotFoundServiceException(
                            lastEntity.get().getPublicId() + " has no child with ID " + entity.getPublicId());
                }
            } else {
                // The first element must be marked as a context
                if (!entity.isContext()) {
                    throw new NotFoundServiceException(
                            "Root element in path is not a context; cannot be the first element in a path");
                }
            }

            lastEntity.set(entity);
        }
    }

    @Override
    public Optional<ResolvedUrl> resolveUrl(String path) {
        try {
            final var resolvedPathComponents = resolveEntitiesFromPath(path);

            validateEntityPath(resolvedPathComponents);

            final var leafElement = resolvedPathComponents.get(resolvedPathComponents.size() - 1);

            final var resolvedUrl = new ResolvedUrl();
            resolvedUrl.setContentUri(leafElement.getContentUri());
            resolvedUrl.setId(leafElement.getPublicId());

            // Create a list of parents with publicId in reversed order, not including the node
            // itself
            resolvedUrl.setParents(resolvedPathComponents.subList(0, resolvedPathComponents.size() - 1).stream()
                    .map(Node::getPublicId).sorted(Collections.reverseOrder()).collect(Collectors.toList()));

            resolvedUrl.setName(leafElement.getName());

            // Generate a string path from the sorted list of parent nodes, a cleaned version of the
            // provided string path parameter
            resolvedUrl.setPath("/" + resolvedPathComponents.stream().map(Node::getPublicId)
                    .map(URI::getSchemeSpecificPart).collect(Collectors.joining("/")));

            return Optional.of(resolvedUrl);
        } catch (NotFoundServiceException e) {
            return Optional.empty();
        }
    }

    private List<Node> resolveEntitiesFromPath(String path) {
        final var returnedList = new ArrayList<Node>();
        final var pathParts = path.split("/+");
        for (String pathPart : pathParts) {
            // Ignore empty parts of the path, including when having leading and/or trailing slashes
            // and multiple slashes
            if (pathPart.equals("")) {
                continue;
            }

            final var resolved = getEntityFromPublicId(URI.create("urn:" + pathPart)).orElseThrow(
                    () -> new NotFoundServiceException("Element with Id " + pathPart + " could not be found"));

            returnedList.add(resolved);
        }

        return returnedList;
    }

    private Optional<Node> getEntityFromPublicId(URI publicId) {
        final var publicIdUrnPart = publicId.getSchemeSpecificPart();
        if (!publicId.getScheme().equals("urn") || publicIdUrnPart == null) {
            throw new InvalidArgumentServiceException("No valid URN provided");
        }

        return nodeRepository.findFirstByPublicId(publicId);
    }
}
