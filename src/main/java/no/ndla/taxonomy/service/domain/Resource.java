package no.ndla.taxonomy.service.domain;


import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Resource extends DomainObject {

    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI contentUri;

    public Resource() {
        setPublicId(URI.create("urn:resource:" + UUID.randomUUID()));
    }

    public Resource name(String name) {
        setName(name);
        return this;
    }

    @OneToMany(mappedBy = "resource")
    private Set<ResourceResourceType> resourceResourceTypes = new HashSet<>();

    public Iterator<ResourceType> getResourceTypes() {
        Iterator<ResourceResourceType> iterator = resourceResourceTypes.iterator();
        return new Iterator<ResourceType>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ResourceType next() {
                return iterator.next().getResourceType();
            }
        };
    }

    public ResourceResourceType addResourceType(ResourceType resourceType) {
        Iterator<ResourceType> resourceTypes = getResourceTypes();
        while (resourceTypes.hasNext()) {
            ResourceType t = resourceTypes.next();
            if (t.getId().equals(resourceType.getId())) {
                throw new DuplicateIdException("Resource with id " + getPublicId() + " is already marked with resource type with id " + resourceType.getPublicId());
            }
        }

        ResourceResourceType resourceResourceType = new ResourceResourceType(this, resourceType);
        resourceResourceTypes.add(resourceResourceType);
        return resourceResourceType;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }
}