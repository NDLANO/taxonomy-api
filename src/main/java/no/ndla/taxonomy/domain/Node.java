/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;

import javax.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@NamedEntityGraph(name = Node.GRAPH, includeAllAttributes = true, attributeNodes = {
        @NamedAttributeNode("translations"), @NamedAttributeNode(value = "metadata"),
        @NamedAttributeNode(value = "parentConnections", subgraph = "parent-connection"),
        @NamedAttributeNode(value = "childConnections", subgraph = "child-connection") }, subgraphs = {
                @NamedSubgraph(name = "parent-connection", attributeNodes = { @NamedAttributeNode("parent"),
                        @NamedAttributeNode(value = "metadata") }),
                @NamedSubgraph(name = "child-connection", attributeNodes = { @NamedAttributeNode("child"),
                        @NamedAttributeNode(value = "metadata") }) })
@Entity
public class Node extends EntityWithPath {
    public static final String GRAPH = "node-with-connections";

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> parentConnections = new TreeSet<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<NodeConnection> childConnections = new TreeSet<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeTranslation> translations = new TreeSet<>();

    @Column
    private URI contentUri;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @Column
    private String ident;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    protected Set<CachedPath> cachedPaths = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "metadata_id")
    private Metadata metadata = new Metadata();

    @Column
    private boolean context;

    @Column
    private boolean root;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new TreeSet<>();

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

        TreeSet<NodeTranslation> trs = new TreeSet<>();
        for (NodeTranslation tr : node.getTranslations()) {
            trs.add(new NodeTranslation(tr, this));
        }
        this.translations = trs;
        TreeSet<ResourceResourceType> rrts = new TreeSet<>();
        for (ResourceResourceType rt : node.getResourceResourceTypes()) {
            ResourceResourceType rrt = new ResourceResourceType();
            if (keepPublicId) {
                rrt.setPublicId(rt.getPublicId());
            }
            rrt.setResource(this);
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

    @Override
    public Set<CachedPath> getCachedPaths() {
        return cachedPaths;
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

    @Override
    public Collection<EntityWithPathConnection> getParentConnections() {
        return parentConnections.stream().map(entity -> (EntityWithPathConnection) entity)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<EntityWithPathConnection> getChildConnections() {
        final Collection<EntityWithPathConnection> children = childConnections.stream()
                .map(entity -> (EntityWithPathConnection) entity).collect(Collectors.toUnmodifiableList());
        return children;
    }

    public Collection<NodeConnection> getChildren() {
        return childConnections;
    }

    public Collection<NodeConnection> getResourceChildren() {
        return childConnections.stream()
                .filter(cc -> cc.getChild().map(child -> child.getNodeType() == NodeType.RESOURCE).orElse(false))
                .collect(Collectors.toUnmodifiableList());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream().map(ResourceResourceType::getResourceType)
                .collect(Collectors.toUnmodifiableList());
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

    public void addResourceResourceType(ResourceResourceType resourceResourceType) {
        if (this.getNodeType() != NodeType.RESOURCE)
            throw new IllegalArgumentException(
                    "ResourceResourceType can only be associated with " + NodeType.RESOURCE.toString());

        this.resourceResourceTypes.add(resourceResourceType);

        if (resourceResourceType.getResource() != this) {
            throw new IllegalArgumentException(
                    "ResourceResourceType must have Resource set before being associated with Resource");
        }
    }

    public void removeResourceResourceType(ResourceResourceType resourceResourceType) {
        this.resourceResourceTypes.remove(resourceResourceType);

        if (resourceResourceType.getResource() == this) {
            resourceResourceType.disassociate();
        }
    }

    public void addChildConnection(NodeConnection nodeConnection) {
        if (nodeConnection.getParent().orElse(null) != this) {
            throw new IllegalArgumentException("Parent must be set on NodeConnection before associating with child");
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

    public void releaseParentConnections() {
        this.parentConnections.clear();
    }

    public Collection<Node> getChildNodes() {
        return childConnections.stream().map(NodeConnection::getChild).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<Node> getParentNode() {
        return parentConnections.stream().map(NodeConnection::getParent).filter(Optional::isPresent).map(Optional::get)
                .findFirst();
    }

    public Collection<Node> getResources() {
        return childConnections.stream().map(NodeConnection::getChild).filter(Optional::isPresent).map(Optional::get)
                .filter(s -> s.getNodeType() == NodeType.RESOURCE).collect(Collectors.toUnmodifiableList());
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

    public NodeTranslation addTranslation(String languageCode) {
        NodeTranslation nodeTranslation = getTranslation(languageCode).orElse(null);
        if (nodeTranslation != null)
            return nodeTranslation;

        nodeTranslation = new NodeTranslation(this, languageCode);
        translations.add(nodeTranslation);
        return nodeTranslation;
    }

    @Override
    public Optional<NodeTranslation> getTranslation(String languageCode) {
        return translations.stream().filter(translation -> translation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    @Override
    public Collection<NodeTranslation> getTranslations() {
        return translations.stream().collect(Collectors.toUnmodifiableList());
    }

    public void clearTranslations() {
        translations.clear();
    }

    public void addTranslation(NodeTranslation nodeTranslation) {
        this.translations.add(nodeTranslation);
        if (nodeTranslation.getNode() != this) {
            nodeTranslation.setNode(this);
        }
    }

    public void removeTranslation(NodeTranslation translation) {
        if (translation.getNode() == this) {
            translations.remove(translation);
            if (translation.getNode() == this) {
                translation.setNode(null);
            }
        }
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void setContext(boolean context) {
        this.context = context;
    }

    @Override
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
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
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
        return context == that.context && root == that.root && Objects.equals(translations, that.translations)
                && Objects.equals(contentUri, that.contentUri) && nodeType == that.nodeType && ident.equals(that.ident)
                && metadata.equals(that.metadata);
    }
}
