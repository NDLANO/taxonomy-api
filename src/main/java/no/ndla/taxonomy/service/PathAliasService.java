package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.PathAlias;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.PathAliasRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
public class PathAliasService implements PathComponentGeneratingService {
    private RootPathService rootPathService;
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;
    private PathAliasRepository pathAliasRepository;

    public PathAliasService(RootPathService rootPathService, TopicRepository topicRepository, ResourceRepository resourceRepository, PathAliasRepository pathAliasRepository) {
        this.rootPathService = rootPathService;
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.pathAliasRepository = pathAliasRepository;
    }

    private Optional<String> leafPath(URI leaf) {
        Resource resource = resourceRepository.findByPublicId(leaf);
        if (resource != null) {
            return Optional.of(generatePathComponent(resource));
        }
        Topic topic = topicRepository.findByPublicId(leaf);
        if (topic != null) {
            return Optional.of(generatePathComponent(topic));
        }
        return Optional.empty();
    }

    private Optional<String> generatePathAlias(URI root, URI leaf) {
        if (root != null) {
            if (leaf != null && !leaf.equals(root)) {
                return rootPathService.generateRootPath(root).flatMap(rootPath ->
                        leafPath(leaf).map(leafPath -> rootPath + "/" + leafPath));
            } else {
                return rootPathService.generateRootPath(root);
            }
        } else if (leaf != null) {
            return rootPathService.generateRootPath(leaf);
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> generatePathAlias(String root, String leaf) {
        if (root != null && !root.startsWith("urn:")) {
            root = "urn:"+root;
        }
        if (leaf != null && !leaf.startsWith("urn:")) {
            leaf = "urn:"+leaf;
        }
        URI rootUri;
        URI leafUri;
        try {
            rootUri = root != null ? new URI(root) : null;
            leafUri = leaf != null ? new URI(leaf) : null;
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        return generatePathAlias(rootUri, leafUri);
    }

    private Optional<String> generatePathAlias(PathAlias pathAlias) {
        return generatePathAlias(pathAlias.getRoot(), pathAlias.getLeaf());
    }

    private static class DecomposedPath {
        private String root;
        private String leaf;
    }

    private Optional<DecomposedPath> decomposePath(String path) {
        var pathIterator = Arrays.stream(path.split("\\/")).filter(comp -> !comp.isBlank()).iterator();
        if (!pathIterator.hasNext()) {
            return Optional.empty();
        }
        var root = pathIterator.next();
        var decomposedPath = new DecomposedPath();
        if (pathIterator.hasNext()) {
            decomposedPath.root = root;
            do {
                decomposedPath.leaf = pathIterator.next();
            } while (pathIterator.hasNext());
        } else {
            decomposedPath.leaf = root;
        }
        return Optional.of(decomposedPath);
    }

    private Optional<String> findAvailableAliasPathVariation(String aliasPath) {
        var timeCompLong = (Instant.now().toEpochMilli() / 60000L) % 1000000L;
        var timeComp = Long.toString(timeCompLong);
        while (timeComp.length() < 6) {
            timeComp = "0"+timeComp;
        }
        for (var i = 0; i < 100; i++) {
            var uniqPart = Integer.toString(i) + "." + timeComp;
            var attempt = aliasPath + "-" + uniqPart;

            if (!pathAliasRepository.findByAlias(attempt).isPresent()) {
                return Optional.of(attempt);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<PathAlias> pathAliasForPath(String path) {
        {
            var pathAlias = pathAliasRepository.findAllByOriginalPath(path)
                    .stream()
                    .reduce((a, b) -> {
                        if (a.getCreated().isBefore(b.getCreated())) {
                            return b;
                        } else if (b.getCreated().isBefore(a.getCreated())) {
                            return a;
                        } else if (a.getReplacedBy() != null) {
                            return b;
                        } else {
                            return a;
                        }
                    })
                    .map(alias -> {
                        while (alias.getReplacedBy() != null) {
                            alias = alias.getReplacedBy();
                        }
                        return alias;
                    })
                    .orElse(null);
            if (pathAlias != null) {
                return Optional.of(pathAlias);
            }
        }
        var pathAlias = new PathAlias();
        {
            var decomposed = decomposePath(path).orElse(null);
            if (decomposed == null) {
                return Optional.empty();
            }
            pathAlias.setRoot(decomposed.root);
            pathAlias.setLeaf(decomposed.leaf);
        }
        {
            var alias = generatePathAlias(pathAlias).flatMap(this::findAvailableAliasPathVariation).orElse(null);
            if (alias == null) {
                return Optional.empty();
            }
            pathAlias.setAlias(alias);
        }
        pathAlias.setOriginalPath(path);
        pathAlias.setCreated(Instant.now());
        return Optional.of(pathAliasRepository.save(pathAlias));
    }

    private <T extends Throwable> void throwIfPresent(Optional<T> optThrow) throws T {
        if (optThrow.isPresent()) {
            throw optThrow.get();
        }
    }

    @Transactional
    public Optional<PathAlias> resolvePath(String alias) throws PathAliasReplacedException {
        var pathAlias = pathAliasRepository.findByAlias(alias);
        throwIfPresent(
                pathAlias
                        .map(PathAlias::getReplacedBy)
                        .map(PathAliasReplacedException::new)
        );
        return pathAlias;
    }

    public static class PathAliasReplacedException extends Exception {
        private PathAlias replacedBy;

        public PathAliasReplacedException(PathAlias replacedBy) {
            this.replacedBy = replacedBy;
        }

        public PathAlias getReplacedBy() {
            return replacedBy;
        }
    }
}
