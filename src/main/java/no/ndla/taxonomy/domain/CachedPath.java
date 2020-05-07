package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "cached_path")
public class CachedPath {
    @Column(name = "is_primary")
    private boolean primary;

    @Column
    @Id
    private UUID id;

    @Column
    private URI publicId;

    @Column
    private String path;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private Topic topic;

    @ManyToOne
    private Resource resource;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Optional<EntityWithPath> getOwningEntity() {
        final var entitiesThatCanBeReturned = new HashSet<EntityWithPath>();

        if (subject != null) {
            entitiesThatCanBeReturned.add(subject);
        }

        if (topic != null) {
            entitiesThatCanBeReturned.add(topic);
        }

        if (resource != null) {
            entitiesThatCanBeReturned.add(resource);
        }

        if (entitiesThatCanBeReturned.size() > 1) {
            throw new IllegalStateException("CachedPath is owned by multiple entities");
        }

        try {
            return Optional.ofNullable(entitiesThatCanBeReturned.iterator().next());
        } catch (NoSuchElementException exception) {
            return Optional.empty();
        }
    }

    public void setOwningEntity(EntityWithPath entity) {
        if (entity == null) {
            this.setSubject(null);
            this.setTopic(null);
            this.setResource(null);

            return;
        }

        if (entity instanceof Subject) {
            this.setSubject((Subject) entity);
        } else if (entity instanceof Topic) {
            this.setTopic((Topic) entity);
        } else if (entity instanceof Resource) {
            this.setResource((Resource) entity);
        } else {
            throw new IllegalArgumentException("Unknown entity of type " + entity.getClass().toString() + " passed as owning entity of CachedPath");
        }

        setPublicId(entity.getPublicId());
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(this.topic);
    }

    public void setTopic(Topic topic) {
        final var oldTopic = this.topic;

        if (topic == null && oldTopic == null) {
            return;
        }

        this.topic = topic;

        if (oldTopic != null && oldTopic != topic) {
            oldTopic.removeCachedPath(this);
        }

        if (topic != null && !topic.getCachedPaths().contains(this)) {
            topic.addCachedPath(this);
        }

        if (topic != null) {
            setSubject(null);
            setResource(null);
        }
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(this.subject);
    }

    public void setSubject(Subject subject) {
        final var oldSubject = this.subject;

        if (subject == null && oldSubject == null) {
            return;
        }

        this.subject = subject;

        if (oldSubject != null && oldSubject != subject) {
            oldSubject.removeCachedPath(this);
        }

        if (subject != null && !subject.getCachedPaths().contains(this)) {
            subject.addCachedPath(this);
        }

        if (subject != null) {
            setTopic(null);
            setResource(null);
        }
    }

    public Optional<Resource> getResource() {
        return Optional.ofNullable(this.resource);
    }

    public void setResource(Resource resource) {
        final var oldResource = this.resource;

        if (resource == null && oldResource == null) {
            return;
        }

        this.resource = resource;

        if (oldResource != null && oldResource != resource) {
            oldResource.removeCachedPath(this);
        }

        if (resource != null && !resource.getCachedPaths().contains(this)) {
            resource.addCachedPath(this);
        }

        if (resource != null) {
            setSubject(null);
            setTopic(null);
        }
    }
}
