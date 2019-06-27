package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.UUID;

@Entity
public class ResourceResourceType extends DomainEntity {

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    protected ResourceResourceType() {
    }

    public ResourceResourceType(Resource resource, ResourceType resourceType) {
        setResource(resource);
        setResourceType(resourceType);
        setPublicId(URI.create("urn:resource-resourcetype:" + UUID.randomUUID()));
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    void setResource(Resource resource) {
        if (this.resource != null && resource != this.resource) {
            this.resource.removeResourceResourceType(this);
        }

        this.resource = resource;

        if (resource != null && !resource.getResourceResourceTypes().contains(this)) {
            resource.addResourceResourceType(this);
        }
    }

    void setResourceType(ResourceType resourceType) {
        if (this.resourceType != null && resourceType != this.resourceType) {
            this.resourceType.removeResourceResourceType(this);
        }

        this.resourceType = resourceType;

        if (resourceType != null && !resourceType.getResourceResourceTypes().contains(this)) {
            resourceType.addResourceResourceType(this);
        }
    }
}
