/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.hypersistence.utils.hibernate.type.json.JsonStringType;
import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@TypeDefs({ @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
        @TypeDef(name = "string-array", typeClass = StringArrayType.class) })
public class Node extends DomainObject implements EntityWithMetadata {
    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> parentConnections = new TreeSet<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> childConnections = new TreeSet<>();

    @Column
    private URI contentUri;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @Column
    private String ident;

    @Column
    @CreationTimestamp
    private Instant created_at;

    @Column
    @UpdateTimestamp
    private Instant updated_at;

    @Type(type = "string-array")
    @Column(name = "cached_paths", columnDefinition = "text[]")
    private String[] cachedPaths;

    @Type(type = "string-array")
    @Column(name = "primary_paths", columnDefinition = "text[]")
    private String[] primaryPaths;

    @Column
    private boolean context;

    @Column
    private boolean root;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new TreeSet<>();

    @Column
    private boolean visible = true;

    @Type(type = "jsonb")
    @Column(name = "translations", columnDefinition = "jsonb")
    private List<JsonTranslation> translations = new ArrayList<>();

    @Type(type = "jsonb")
    @Column(name = "grepcodes", columnDefinition = "jsonb")
    private Set<JsonGrepCode> grepcodes = new HashSet<>();

    @Type(type = "jsonb")
    @Column(name = "customfields", columnDefinition = "jsonb")
    private Map<String, String> customfields = new HashMap<>();

    // Needed for hibernate
    public Node() {
    }

    public Node(NodeType nodeType) {
        setNodeType(nodeType);
        setIdent(UUID.randomUUID().toString());
        updatePublicID();
    }

    public Node(Node node) {
        this(node, true);
    }

    public Node(Node node, boolean keepPublicId) {
        this.contentUri = node.getContentUri();
        this.nodeType = node.getNodeType();
        this.ident = node.getIdent();
        this.context = node.isContext();
        this.root = node.isRoot();

        if (keepPublicId) {
            setPublicId(node.getPublicId());
        } else {
            setIdent(UUID.randomUUID().toString());
            updatePublicID();
        }

        this.translations = node.getTranslations().stream().map(JsonTranslation::new).toList();
        TreeSet<ResourceResourceType> rrts = new TreeSet<>();
        for (ResourceResourceType rt : node.getResourceResourceTypes()) {
            ResourceResourceType rrt = new ResourceResourceType();
            if (keepPublicId) {
                rrt.setPublicId(rt.getPublicId());
            }
            rrt.setNode(this);
            rrt.setResourceType(rt.getResourceType());
            rrts.add(rrt);
        }
        this.resourceResourceTypes = rrts;
        setMetadata(new Metadata(node.getMetadata()));
        setName(node.getName());
    }

    private void updatePublicID() {
        super.setPublicId(URI.create("urn:" + nodeType.getName() + ":" + getIdent()));
    }

    public Set<CachedPath> getCachedPaths() {
        return CachedPath.fromPaths(this.primaryPaths, this.cachedPaths);
    }

    public void setCachedPaths(Set<CachedPath> cachedPaths) {
        this.primaryPaths = cachedPaths.stream().filter(CachedPath::isPrimary).map(CachedPath::getPath)
                .toArray(String[]::new);
        this.cachedPaths = cachedPaths.stream().filter(s -> !s.isPrimary()).map(CachedPath::getPath)
                .toArray(String[]::new);
    }

    public void addCachedPath(CachedPath cachedPath) {
        this.getCachedPaths().add(cachedPath);

        if (cachedPath.getNode().orElse(null) != this) {
            cachedPath.setNode(this);
        }
    }

    @Deprecated
    public void removeCachedPath(CachedPath cachedPath) {
        this.getCachedPaths().remove(cachedPath);

        if (cachedPath.getNode().orElse(null) == this) {
            cachedPath.setNode(null);
        }
    }

    public Optional<String> getPrimaryPath() {
        return getCachedPaths().stream().filter(CachedPath::isPrimary).map(CachedPath::getPath).findFirst();
    }

    public Optional<String> getPathByContext(URI contextPublicId) {
        var cp = getCachedPaths();
        return cp.stream().sorted((cachedUrl1, cachedUrl2) -> {
            final var path1 = cachedUrl1.getPath();
            final var path2 = cachedUrl2.getPath();

            final var path1MatchesContext = path1.startsWith("/" + contextPublicId.getSchemeSpecificPart());
            final var path2MatchesContext = path2.startsWith("/" + contextPublicId.getSchemeSpecificPart());

            final var path1IsPrimary = cachedUrl1.isPrimary();
            final var path2IsPrimary = cachedUrl2.isPrimary();

            if (path1IsPrimary && path2IsPrimary && path1MatchesContext && path2MatchesContext) {
                return 0;
            }

            if (path1MatchesContext && path2MatchesContext && path1IsPrimary) {
                return -1;
            }

            if (path1MatchesContext && path2MatchesContext && path2IsPrimary) {
                return 1;
            }

            if (path1MatchesContext && !path2MatchesContext) {
                return -1;
            }

            if (path2MatchesContext && !path1MatchesContext) {
                return 1;
            }

            if (path1IsPrimary && !path2IsPrimary) {
                return -1;
            }

            if (path2IsPrimary && !path1IsPrimary) {
                return 1;
            }

            return 0;
        }).map(CachedPath::getPath).findFirst();
    }

    public TreeSet<String> getAllPaths() {
        return getCachedPaths().stream().map(CachedPath::getPath).collect(Collectors.toCollection(TreeSet::new));
    }

    /*
     * In the old code the primary URL for topics was special since it would try to return a context URL (a topic
     * behaving as a subject) rather than a subject URL if that is available. Trying to re-implement it by sorting the
     * context URLs first by the same path comparing as the old code
     */
    /*
     * public Optional<String> getPrimaryPath() { return getCachedPaths() .stream()
     * .map(CachedPath::getPath).min((path1, path2) -> { if (path1.startsWith("/topic") && path2.startsWith("/topic")) {
     * return 0; }
     *
     * if (path1.startsWith("/topic")) { return -1; }
     *
     * if (path2.startsWith("/topic")) { return 1; }
     *
     * return 0; }); }
     */

    public Optional<NodeConnection> getParentConnection() {
        return this.getParentConnections().stream().findFirst();
    }

    public Collection<NodeConnection> getParentConnections() {
        return parentConnections.stream().map(entity -> (NodeConnection) entity).toList();
    }

    public Set<NodeConnection> getParentNodeConnections() {
        return this.parentConnections;
    }

    public Collection<NodeConnection> getChildConnections() {
        return childConnections.stream().map(entity -> (NodeConnection) entity).collect(Collectors.toSet());
    }

    public Collection<NodeConnection> getChildren() {
        return childConnections;
    }

    public Collection<NodeConnection> getResourceChildren() {
        return childConnections.stream()
                .filter(cc -> cc.getChild().map(child -> child.getNodeType() == NodeType.RESOURCE).orElse(false))
                .collect(Collectors.toSet());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream().map(ResourceResourceType::getResourceType)
                .collect(Collectors.toSet());
    }

    public Collection<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes;
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        if (getResourceTypes().contains(resourceType)) {
            throw new DuplicateIdException("Resource with id " + getPublicId()
                    + " is already marked with resource type with id " + resourceType.getPublicId());
        }

        ResourceResourceType resourceResourceType = ResourceResourceType.create(this, resourceType);
        addResourceResourceType(resourceResourceType);
        return resourceResourceType;
    }

    public void removeResourceType(ResourceType resourceType) {
        var resourceResourceType = getResourceType(resourceType);
        if (resourceResourceType.isEmpty())
            throw new ChildNotFoundException(
                    "Resource with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());

        resourceResourceTypes.remove(resourceResourceType.get());
    }

    private Optional<ResourceResourceType> getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType))
                return Optional.of(resourceResourceType);
        }
        return Optional.empty();
    }

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        if (this.getNodeType() != NodeType.RESOURCE)
            throw new IllegalArgumentException(
                    "ResourceResourceType can only be associated with " + NodeType.RESOURCE.toString());

        this.resourceResourceTypes.add(resourceResourceType);

        if (resourceResourceType.getNode() != this) {
            throw new IllegalArgumentException(
                    "ResourceResourceType must have Resource set before being associated with Resource");
        }
    }

    public void removeResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType);

        if (resourceResourceType.getNode() == this) {
            resourceResourceType.disassociate();
        }
    }

    public void addChildConnection(NodeConnection nodeConnection) {
        if (nodeConnection.getParent().orElse(null) != this) {
            throw new IllegalArgumentException("Parent must be set on NodeConnection before associating with child");
        }
        if (this.nodeType == NodeType.RESOURCE) {
            throw new IllegalArgumentException("'" + NodeType.RESOURCE + "' nodes cannot have children");
        }

        this.childConnections.add(nodeConnection);
    }

    public void removeChildConnection(NodeConnection nodeConnection) {
        this.childConnections.remove(nodeConnection);

        nodeConnection.disassociate();
    }

    public void addParentConnection(NodeConnection nodeConnection) {
        if (nodeConnection.getChild().orElse(null) != this) {
            throw new IllegalArgumentException("Child must be set on NodeConnection before associating with Parent");
        }

        this.parentConnections.add(nodeConnection);
    }

    public void removeParentConnection(NodeConnection nodeConnection) {
        this.parentConnections.remove(nodeConnection);

        nodeConnection.disassociate();
    }

    public Collection<Node> getChildNodes() {
        return childConnections.stream().map(NodeConnection::getChild).filter(Optional::isPresent).map(Optional::get)
                .toList();
    }

    public Optional<Node> getParentNode() {
        return parentConnections.stream().map(NodeConnection::getParent).filter(Optional::isPresent).map(Optional::get)
                .findFirst();
    }

    public Collection<Node> getParentNodes() {
        return parentConnections.stream().map(NodeConnection::getParent).filter(Optional::isPresent).map(Optional::get)
                .toList();
    }

    public Collection<Node> getResources() {
        return childConnections.stream().map(NodeConnection::getChild).filter(Optional::isPresent).map(Optional::get)
                .filter(s -> s.getNodeType() == NodeType.RESOURCE).toList();
    }

    public void setIdent(String ident) {
        this.ident = ident;
        updatePublicID();
    }

    public String getIdent() {
        return ident;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        updatePublicID();
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    public boolean isContext() {
        return root || context;
    }

    @Override
    public String getEntityName() {
        return nodeType.getName();
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    @Override
    public void setPublicId(URI publicId) {
        final String[] idParts = publicId.toString().split(":");
        setIdent(String.join(":", Arrays.copyOfRange(idParts, 2, idParts.length)));
    }

    public Node name(String name) {
        setName(name);
        return this;
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(this);
    }

    @Override
    public Set<JsonGrepCode> getGrepCodes() {
        return this.grepcodes;
    }

    @Override
    public void setCustomField(String key, String value) {
        this.customfields.put(key, value);
    }

    @Override
    public void unsetCustomField(String key) {
        this.customfields.remove(key);
    }

    @Override
    public void setGrepCodes(Set<JsonGrepCode> codes) {
        this.grepcodes = codes;
    }

    @Override
    public void setCustomFields(Map<String, String> customFields) {
        this.customfields = customFields;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updated_at = updatedAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.created_at = createdAt;
    }

    @Override
    public boolean getVisible() {
        return this.visible;
    }

    @Override
    public Instant getCreatedAt() {
        return this.created_at;
    }

    @Override
    public Instant getUpdatedAt() {
        return this.updated_at;
    }

    @PreRemove
    void preRemove() {
        Set.copyOf(childConnections).forEach(NodeConnection::disassociate);
        Set.copyOf(parentConnections).forEach(NodeConnection::disassociate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node that = (Node) o;
        return context == that.context && root == that.root && Objects.equals(contentUri, that.contentUri)
                && nodeType == that.nodeType && ident.equals(that.ident)
                && Objects.equals(translations, that.translations)
                && Objects.equals(this.customfields, that.customfields) && this.visible == that.visible
                && Objects.equals(this.grepcodes, that.grepcodes);
    }

    public Optional<Node> getPrimaryNode() {
        for (var node : this.parentConnections) {
            if (node.isPrimary().orElse(false))
                return node.getParent();
        }
        return Optional.empty();
    }

    private List<NodePath> getParentPaths(List<NodePath> oldBasePaths, Node node,
            Optional<NodeConnection> connectionToChild) {
        var basePaths = oldBasePaths.stream().map(p -> p.add(node, connectionToChild)).toList();

        var parentConnections = node.getParentNodeConnections().stream().filter(nc -> nc.getParent().isPresent())
                .toList();
        if (parentConnections.isEmpty()) {
            return basePaths;
        }

        var paths = new ArrayList<NodePath>();

        for (var parentConnection : parentConnections) {
            var maybeParent = parentConnection.getParent();
            if (maybeParent.isEmpty())
                continue;
            var parent = maybeParent.get();

            var result = this.getParentPaths(basePaths, parent, Optional.of(parentConnection));
            paths.addAll(result);
        }

        return paths;
    }

    public Map<String, String> getCustomFields() {
        return this.customfields;
    }

    public void addGrepCode(String code) {
        var now = Instant.now().toString();
        var newGrepCode = new JsonGrepCode(code, now, now);
        this.grepcodes.add(newGrepCode);
    }

    public List<NodePath> buildPaths() {
        var np = new NodePath();
        var paths = getParentPaths(List.of(np), this, Optional.empty());
        return paths.stream().sorted(Comparator.comparing(NodePath::toString)).toList();
    }

    public List<String> buildCrumbs(String languageCode) {
        List<String> parentCrumbs = this.getParentConnection().flatMap(parentConnection -> parentConnection
                .getConnectedParent().map(parent -> buildCrumbs(parent, languageCode))).orElse(List.of());

        var crumbs = new ArrayList<>(parentCrumbs);
        var name = this.getTranslation(languageCode).map(Translation::getName).orElse(this.getName());
        crumbs.add(name);
        return crumbs;
    }

    private List<String> buildCrumbs(Node entity, String languageCode) {
        List<String> parentCrumbs = entity.getParentConnection().flatMap(parentConnection -> parentConnection
                .getConnectedParent().map(parent -> buildCrumbs(parent, languageCode))).orElse(List.of());

        var crumbs = new ArrayList<>(parentCrumbs);
        var name = entity.getTranslation(languageCode).map(Translation::getName).orElse(entity.getName());
        crumbs.add(name);
        return crumbs;
    }

    @Override
    public List<JsonTranslation> getTranslations() {
        return this.translations;
    }

    @Override
    public void setTranslations(List<JsonTranslation> translations) {
        this.translations = translations;
    }
}
