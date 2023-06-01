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
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import org.hibernate.annotations.*;

@Entity
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonStringType.class),
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
    @TypeDef(name = "string-array", typeClass = StringArrayType.class)
})
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

    @Type(type = "jsonb")
    @Column(name = "contexts", columnDefinition = "jsonb")
    private Set<Context> contexts = new HashSet<>();

    // Needed for hibernate
    public Node() {}

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

        this.translations =
                node.getTranslations().stream().map(JsonTranslation::new).toList();
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

    public Optional<String> getPrimaryPath() {
        return getContexts().stream()
                .filter(Context::isPrimary)
                .map(Context::path)
                .findFirst();
    }

    /**
     * Picks a context based on parameters. If no parameters or no root matching, pick by matching path and primary
     *
     * @param contextId
     *            If this is present, return the context with corresponding id
     * @param root
     *            If this is present, return context with this publicId as root. Else pick context containing roots
     *            publicId.
     *
     * @return Context
     */
    public Optional<Context> pickContext(Optional<String> contextId, Optional<Node> root) {
        var contexts = getContexts();
        var maybeContext = contextId.flatMap(
                id -> contexts.stream().filter(c -> c.contextId().equals(id)).findFirst());
        if (maybeContext.isPresent()) {
            return maybeContext;
        }
        var maybeRoot = root.flatMap(node -> contexts.stream()
                .filter(c -> c.rootId().equals(node.getPublicId().toString()))
                .findFirst());
        if (maybeRoot.isPresent()) {
            return maybeRoot;
        }
        return contexts.stream().min((context1, context2) -> {
            final var inPath1 = context1.path()
                    .contains(root.map(node -> node.getPublicId().getSchemeSpecificPart())
                            .orElse("other"));
            final var inPath2 = context2.path()
                    .contains(root.map(node -> node.getPublicId().getSchemeSpecificPart())
                            .orElse("other"));

            if (inPath1 && inPath2) {
                if (context1.isPrimary() && context2.isPrimary()) {
                    return 0;
                }
                if (context1.isPrimary()) {
                    return -1;
                }
                if (context2.isPrimary()) {
                    return 1;
                }
            }
            if (inPath1 && !inPath2) {
                return -1;
            }
            if (inPath2 && !inPath1) {
                return 1;
            }
            if (context1.isPrimary() && !context2.isPrimary()) {
                return -1;
            }
            if (context2.isPrimary() && !context1.isPrimary()) {
                return 1;
            }
            return 0;
        });
    }

    public TreeSet<String> getAllPaths() {
        return getContexts().stream().map(Context::path).collect(Collectors.toCollection(TreeSet::new));
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

    public Collection<NodeConnection> getParentConnections() {
        return this.parentConnections.stream().toList();
    }

    public Collection<NodeConnection> getChildConnections() {
        return new HashSet<>(childConnections);
    }

    public Collection<NodeConnection> getResourceChildren() {
        return this.childConnections.stream()
                .filter(cc -> cc.getChild()
                        .map(child -> child.getNodeType() == NodeType.RESOURCE)
                        .orElse(false))
                .collect(Collectors.toSet());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream()
                .map(ResourceResourceType::getResourceType)
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

        this.resourceResourceTypes.remove(resourceResourceType.get());
    }

    private Optional<ResourceResourceType> getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType)) return Optional.of(resourceResourceType);
        }
        return Optional.empty();
    }

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        if (this.getNodeType() != NodeType.RESOURCE)
            throw new IllegalArgumentException("ResourceResourceType can only be associated with " + NodeType.RESOURCE);

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
        return childConnections.stream()
                .map(NodeConnection::getChild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getParentNodes() {
        return parentConnections.stream()
                .map(NodeConnection::getParent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Collection<Node> getResources() {
        return childConnections.stream()
                .map(NodeConnection::getChild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> s.getNodeType() == NodeType.RESOURCE)
                .toList();
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

    public Optional<String> getContextType() {
        if (contentUri == null) return Optional.empty();
        if (contentUri.getSchemeSpecificPart().startsWith("learningpath")) return Optional.of("learningpath");
        if (nodeType.equals(NodeType.TOPIC)) return Optional.of("topic-article");
        if (contentUri.getSchemeSpecificPart().startsWith("article")) return Optional.of("standard");
        return Optional.empty();
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
    public boolean isVisible() {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node that = (Node) o;
        return context == that.context
                && root == that.root
                && Objects.equals(contentUri, that.contentUri)
                && nodeType == that.nodeType
                && ident.equals(that.ident)
                && Objects.equals(translations, that.translations)
                && Objects.equals(this.customfields, that.customfields)
                && this.visible == that.visible
                && Objects.equals(this.grepcodes, that.grepcodes);
    }

    public Optional<Node> getPrimaryNode() {
        for (var node : this.parentConnections) {
            if (node.isPrimary().orElse(false)) return node.getParent();
        }
        return Optional.empty();
    }

    public Map<String, String> getCustomFields() {
        return this.customfields;
    }

    public void setContexts(Set<Context> contexts) {
        this.contexts = contexts;
    }

    public Set<Context> getContexts() {
        return this.contexts;
    }

    public void addGrepCode(String code) {
        var now = Instant.now().toString();
        var newGrepCode = new JsonGrepCode(code, now, now);
        this.grepcodes.add(newGrepCode);
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
