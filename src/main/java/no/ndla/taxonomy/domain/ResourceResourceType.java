/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Entity
public class ResourceResourceType extends DomainEntity implements Comparable<ResourceResourceType> {

    @ManyToOne(cascade = { CascadeType.MERGE })
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(cascade = { CascadeType.MERGE })
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    public ResourceResourceType() {
        setPublicId(URI.create("urn:resource-resourcetype:" + UUID.randomUUID()));
    }

    public static ResourceResourceType create(Resource resource, ResourceType resourceType) {
        final var resourceResourceType = new ResourceResourceType();

        resourceResourceType.resource = resource;
        resourceResourceType.resourceType = resourceType;

        resource.addResourceResourceType(resourceResourceType);
        resourceType.addResourceResourceType(resourceResourceType);

        return resourceResourceType;
    }

    public void disassociate() {
        final var resource = this.resource;
        final var resourceType = this.resourceType;

        this.resourceType = null;
        this.resource = null;

        if (resource != null) {
            resource.removeResourceResourceType(this);
        }

        if (resourceType != null) {
            resourceType.removeResourceResourceType(this);
        }
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @PreRemove
    void preRemove() {
        this.disassociate();
    }

    @Override
    public int compareTo(ResourceResourceType o) {
        if (this.resourceType == null || o.resourceType == null) {
            return 0;
        }
        return this.getResourceType().compareTo(o.getResourceType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ResourceResourceType that = (ResourceResourceType) o;
        return Objects.equals(resource.getPublicId(), that.resource.getPublicId())
                && Objects.equals(resourceType.getPublicId(), that.resourceType.getPublicId());
    }
}
