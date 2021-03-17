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

        if (entity instanceof Topic) {
            this.setTopic((Topic) entity);
        } else if (entity instanceof Resource) {
            this.setResource((Resource) entity);
        } else {
            throw new IllegalArgumentException("Unknown entity of type " + entity.getClass().toString() + " passed as owning entity of CachedPath");
        }

        setPublicId(entity.getPublicId());
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(this.topic)
                .filter(topic -> topic.getPublicId() == null || !topic.getPublicId().toString().startsWith("urn:subject:"));
    }

    public void setTopic(Topic topic) {
        final var oldTopic = this.topic;

        if (topic == null) {
            if (oldTopic == null) {
                return;
            }
            final var oldTopicId = oldTopic.getPublicId();
            if (oldTopicId != null && oldTopicId.toString().startsWith("urn:subject:")) {
                // The current topic is a subject, so setTopic(null) is a NOP
                return;
            }
        }

        this.topic = topic;

        if (oldTopic != null && oldTopic != topic) {
            oldTopic.removeCachedPath(this);
        }

        if (topic != null && !topic.getCachedPaths().contains(this)) {
            topic.addCachedPath(this);
        }

        if (topic != null) {
            setResource(null);
        }
    }

    public Optional<Topic> getSubject() {
        return Optional.ofNullable(this.topic)
                .filter(topic -> topic.getPublicId() != null && topic.getPublicId().toString().startsWith("urn:subject:"));
    }

    public void setSubject(Topic subject) {
        final var oldSubject = this.topic;

        if (subject == null) {
            if (oldSubject == null) {
                return;
            }
            final var oldSubjectId = oldSubject.getPublicId();
            if (oldSubjectId == null || !oldSubjectId.toString().startsWith("urn:subject:")) {
                // The old topic is of type topic, setSubject(null) is a NOP
                return;
            }
        } else if (subject.getPublicId() == null || !subject.getPublicId().toString().startsWith("urn:subject:")) {
            throw new RuntimeException("The type of topic should be subject");
        }

        this.topic = subject;

        if (oldSubject != null && oldSubject != subject) {
            oldSubject.removeCachedPath(this);
        }

        if (subject != null && !subject.getCachedPaths().contains(this)) {
            subject.addCachedPath(this);
        }

        if (subject != null) {
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
