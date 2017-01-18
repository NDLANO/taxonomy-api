package no.ndla.taxonomy.service.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class ResourceType extends DomainObject {
    public ResourceType() {
        setPublicId(URI.create("urn:resource-type:" + UUID.randomUUID()));
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ResourceType parent;

    @OneToMany(mappedBy = "parent")
    private Set<ResourceType> subtypes = new HashSet<>();

    public ResourceType name(String name) {
        setName(name);
        return this;
    }

    public ResourceType getParent() {
        return parent;
    }


    public void setParent(ResourceType parent) {
        this.parent = parent;
    }

    public ResourceType parent(ResourceType parent) {
        setParent(parent);
        return this;
    }


    public Set<ResourceType> getSubtypes() {
        return subtypes;
    }

}
