/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.domain.exceptions.ChildNotFoundException;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;

import javax.persistence.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@NamedEntityGraph(name = Resource.GRAPH, includeAllAttributes = true, attributeNodes = { @NamedAttributeNode(value = "metadata"),
        @NamedAttributeNode(value = "resourceTranslations"),
        @NamedAttributeNode(value = "resourceResourceTypes", subgraph = "resourceResourceTypes"),
        @NamedAttributeNode(value = "nodes", subgraph = "node-with-connections") }, subgraphs = {
                @NamedSubgraph(name = "resourceResourceTypes", attributeNodes = {
                        @NamedAttributeNode(value = "resourceType", subgraph = "resourceType") }),
                @NamedSubgraph(name = "resourceType", attributeNodes = {
                        @NamedAttributeNode("resourceTypeTranslations"),
                        @NamedAttributeNode(value = "parent", subgraph = "resourceTypeParent") }),
                @NamedSubgraph(name = "resourceTypeParent", attributeNodes = {
                        @NamedAttributeNode("resourceTypeTranslations") }) })
@Entity
public class Resource extends EntityWithPath {
    public static final String GRAPH = "resource-with-data";

    @Column
    private URI contentUri;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceTranslation> resourceTranslations = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeResource> nodes = new HashSet<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    protected Set<CachedPath> cachedPaths = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "metadata_id")
    private Metadata metadata = new Metadata();

    @Override
    public Set<CachedPath> getCachedPaths() {
        return cachedPaths;
    }

    @Override
    public Set<EntityWithPathConnection> getParentConnections() {
        return nodes.stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<EntityWithPathConnection> getChildConnections() {
        return Set.of();
    }

    private URI generateId() {
        return URI.create("urn:resource:" + UUID.randomUUID());
    }

    public Resource() {
        setPublicId(generateId());
    }

    public Resource(Resource resource, boolean keepPublicId) {
        this.contentUri = resource.getContentUri();
        Set<ResourceTranslation> trs = new HashSet<>();
        for (ResourceTranslation tr : resource.getTranslations()) {
            trs.add(new ResourceTranslation(tr, this));
        }
        this.resourceTranslations = trs;
        Set<ResourceResourceType> rrts = new HashSet<>();
        for (ResourceResourceType rt : resource.getResourceResourceTypes()) {
            ResourceResourceType rrt = new ResourceResourceType();
            if (keepPublicId) {
                rrt.setPublicId(rt.getPublicId());
            }
            rrt.setResource(this);
            rrt.setResourceType(rt.getResourceType());
            rrts.add(rrt);
        }
        this.resourceResourceTypes = rrts;
        setMetadata(new Metadata(resource.getMetadata()));
        setName(resource.getName());
        setPublicId(keepPublicId ? resource.getPublicId() : generateId());
    }

    public Collection<Node> getNodes() {
        return getNodeResources().stream().map(NodeResource::getNode).map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<ResourceType> getResourceTypes() {
        return getResourceResourceTypes().stream().map(ResourceResourceType::getResourceType)
                .collect(Collectors.toUnmodifiableSet());
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

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public ResourceTranslation addTranslation(String languageCode) {
        ResourceTranslation resourceTranslation = getTranslation(languageCode).orElse(null);
        if (resourceTranslation != null)
            return resourceTranslation;

        resourceTranslation = new ResourceTranslation(this, languageCode);
        resourceTranslations.add(resourceTranslation);
        return resourceTranslation;
    }

    @Override
    public Optional<ResourceTranslation> getTranslation(String languageCode) {
        return resourceTranslations.stream()
                .filter(resourceTranslation -> resourceTranslation.getLanguageCode().equals(languageCode)).findFirst();
    }

    public Set<ResourceTranslation> getTranslations() {
        return resourceTranslations.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void clearTranslations() {
        resourceTranslations.clear();
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void addTranslation(ResourceTranslation resourceTranslation) {
        this.resourceTranslations.add(resourceTranslation);
        if (resourceTranslation.getResource() != this) {
            resourceTranslation.setResource(this);
        }
    }

    public void removeTranslation(ResourceTranslation translation) {
        if (translation.getResource() == this) {
            resourceTranslations.remove(translation);
            if (translation.getResource() == this) {
                translation.setResource(null);
            }
        }
    }

    public Optional<Node> getPrimaryNode() {
        for (NodeResource node : nodes) {
            if (node.isPrimary().orElse(false))
                return node.getNode();
        }
        return Optional.empty();
    }

    public void removeResourceType(ResourceType resourceType) {
        ResourceResourceType resourceResourceType = getResourceType(resourceType);
        if (resourceResourceType == null)
            throw new ChildNotFoundException(
                    "Resource with id " + this.getPublicId() + " is not of type " + resourceType.getPublicId());

        resourceResourceTypes.remove(resourceResourceType);
    }

    private ResourceResourceType getResourceType(ResourceType resourceType) {
        for (ResourceResourceType resourceResourceType : resourceResourceTypes) {
            if (resourceResourceType.getResourceType().equals(resourceType))
                return resourceResourceType;
        }
        return null;
    }

    public Set<ResourceResourceType> getResourceResourceTypes() {
        return this.resourceResourceTypes.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Set<NodeResource> getNodeResources() {
        return this.nodes.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeNodeResource(NodeResource nodeResource) {
        this.nodes.remove(nodeResource);

        if (nodeResource.getResource().orElse(null) == this) {
            nodeResource.disassociate();
        }
    }

    public void addNodeResource(NodeResource nodeResource) {
        this.nodes.add(nodeResource);

        if (nodeResource.getResource().orElse(null) != this) {
            throw new IllegalArgumentException("NodeResource must have Resource relation set before adding");
        }
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
        Set.copyOf(resourceResourceTypes).forEach(ResourceResourceType::disassociate);
        Set.copyOf(nodes).forEach(NodeResource::disassociate);
    }

    @Override
    public boolean isContext() {
        return false;
    }
}
