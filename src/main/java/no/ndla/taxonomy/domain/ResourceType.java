package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.*;

@Entity
public class ResourceType extends DomainObject {

    public ResourceType() {
        setPublicId(URI.create("urn:resourcetype:" + UUID.randomUUID()));
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ResourceType parent;

    @OneToMany(mappedBy = "parent")
    private Set<ResourceType> subtypes = new HashSet<>();

    @OneToMany(mappedBy = "resourceType")
    Set<ResourceTypeTranslation> resourceTypeTranslations = new HashSet<>();

    public ResourceType name(String name) {
        setName(name);
        return this;
    }

    public Optional<ResourceType> getParent() {
        return Optional.ofNullable(parent);
    }

    public void setParent(ResourceType parent) {
        this.parent = parent;
        if (parent != null) {
            parent.addSubtype(this);
        }
    }

    public ResourceType parent(ResourceType parent) {
        setParent(parent);
        return this;
    }

    public void addSubtype(ResourceType subtype) {
        subtypes.add(subtype);
    }

    public Iterator<ResourceType> getSubtypes() {
        Iterator<ResourceType> iterator = subtypes.iterator();

        return new Iterator<ResourceType>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ResourceType next() {
                return iterator.next();
            }
        };
    }

    public ResourceTypeTranslation addTranslation(String languageCode) {
        final var existingResourceTypeTranslation = getTranslation(languageCode);

        if (existingResourceTypeTranslation.isPresent()) {
            return existingResourceTypeTranslation.get();
        }

        final var resourceTypeTranslation = new ResourceTypeTranslation(this, languageCode);
        resourceTypeTranslations.add(resourceTypeTranslation);
        return resourceTypeTranslation;
    }

    public Optional<ResourceTypeTranslation> getTranslation(String languageCode) {
        return resourceTypeTranslations.stream()
                .filter(resourceTypeTranslation -> resourceTypeTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Iterator<ResourceTypeTranslation> getTranslations() {
        return resourceTypeTranslations.iterator();
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(resourceTypeTranslations::remove);
    }
}
