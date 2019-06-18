package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicResource extends DomainEntity implements Rankable {

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    protected TopicResource() {
    }

    public TopicResource(Topic topic, Resource resource) {
        this.topic = topic;
        this.resource = resource;
        setPublicId(URI.create("urn:topic-resource:" + UUID.randomUUID()));
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Topic getTopic() {
        return topic;
    }

    public Resource getResource() {
        return resource;
    }

    public String toString() {
        return "TopicResource: { " + topic.getName() + " " + topic.getPublicId() + " -> " + resource.getName() + " " + resource.getPublicId() + " " + (isPrimary() ? "P" : "") + " " + rank + "}";
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @PreUpdate
    @PreRemove
    @PrePersist
    void updateObjectsUpdatedAt() {
        if (topic != null) {
            topic.updateUpdatedAt();
        }
        if (resource != null) {
            resource.updateUpdatedAt();
        }
    }
}
