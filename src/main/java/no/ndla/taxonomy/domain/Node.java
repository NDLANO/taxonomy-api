package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class Node extends EntityWithPath {
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeConnection> parentConnections = new HashSet<>();

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeConnection> childConnections = new HashSet<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeResource> nodeResources = new HashSet<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeTranslation> translations = new HashSet<>();

    @Column
    private URI contentUri;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    @Column
    private String ident;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    protected Set<CachedPath> cachedPaths = new HashSet<>();

    @Column
    private boolean context;

    // Needed for hibernate
    public Node() {}

    public Node(NodeType nodeType) {
        setIdent(UUID.randomUUID().toString());
        updatePublicID();
    }

    private void updatePublicID() {
        setPublicId(URI.create("urn:" + nodeType.getName() + ":" + getIdent()));
    }

    @Override
    public Set<CachedPath> getCachedPaths() {
        return cachedPaths;
    }

    /*
        In the old code the primary URL for topics was special since it would try to return
        a context URL (a topic behaving as a subject) rather than a subject URL if that is available.
        Trying to re-implement it by sorting the context URLs first by the same path comparing as the old code
     */
    /*public Optional<String> getPrimaryPath() {
        return getCachedPaths()
                .stream()
                .map(CachedPath::getPath).min((path1, path2) -> {
                    if (path1.startsWith("/topic") && path2.startsWith("/topic")) {
                        return 0;
                    }

                    if (path1.startsWith("/topic")) {
                        return -1;
                    }

                    if (path2.startsWith("/topic")) {
                        return 1;
                    }

                    return 0;
                });
    }*/

    @Override
    public Set<EntityWithPathConnection> getParentConnections() {
        return parentConnections.stream()
                .map(entity -> (EntityWithPathConnection) entity)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<EntityWithPathConnection> getChildConnections() {
        final var toReturn = new HashSet<EntityWithPathConnection>();

        toReturn.addAll(getChildrenConnections());
        toReturn.addAll(getNodeResources());

        return toReturn;
    }

    public Set<NodeConnection> getChildrenConnections() {
        return this.childConnections.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Optional<NodeConnection> getParentConnection() {
        return this.parentConnections.stream().findFirst();
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

    public Set<Node> getChildNodes() {
        return childConnections.stream()
                .map(NodeConnection::getChild)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<Node> getParentNode() {
        return parentConnections.stream()
                .map(NodeConnection::getParent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public Set<NodeResource> getNodeResources() {
        return this.nodeResources
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    public void addNodeResource(NodeResource nodeResource) {
        if (nodeResource.getNode().orElse(null) != this) {
            throw new IllegalArgumentException("NodeResource must have Node set before it can be associated with Resource");
        }

        this.nodeResources.add(nodeResource);
    }

    public void removeNodeResource(NodeResource nodeResource) {
        this.nodeResources.remove(nodeResource);

        final var resource = nodeResource.getResource();

        if (nodeResource.getNode().orElse(null) == this) {
            nodeResource.disassociate();
        }
    }

    public Set<Resource> getResources() {
        return nodeResources.stream()
                .map(NodeResource::getResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setIdent(String ident) {
        this.ident = ident;
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
        if (nodeTranslation != null) return nodeTranslation;

        nodeTranslation = new NodeTranslation(this, languageCode);
        translations.add(nodeTranslation);
        return nodeTranslation;
    }

    public Optional<NodeTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(translation -> translation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Set<NodeTranslation> getTranslations() {
        return translations.stream().collect(Collectors.toUnmodifiableSet());
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
        return context;
    }

    @PreRemove
    void preRemove() {
        Set.copyOf(childConnections).forEach(NodeConnection::disassociate);
        Set.copyOf(parentConnections).forEach(NodeConnection::disassociate);
        Set.copyOf(nodeResources).forEach(NodeResource::disassociate);
    }
}
