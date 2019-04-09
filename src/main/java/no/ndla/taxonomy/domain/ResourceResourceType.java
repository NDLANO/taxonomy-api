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

    public ResourceResourceType(Resource resource, ResourceType resourceType, URI publicId) {
        this.resource = resource;
        this.resourceType = resourceType;
        setPublicId(publicId);
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
