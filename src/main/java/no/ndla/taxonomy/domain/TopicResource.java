package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicResource extends DomainEntity implements Rankable {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    public TopicResource() {
        setPublicId(URI.create("urn:topic-resource:" + UUID.randomUUID()));
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setTopic(Topic topic) {
        final var previousTopic = this.topic;

        this.topic = topic;

        if (previousTopic != null && previousTopic != topic) {
            previousTopic.removeTopicResource(this);
        }

        if (topic != null && !topic.getTopicResources().contains(this)) {
            topic.addTopicResource(this);
        }
    }

    public Optional<Resource> getResource() {
        return Optional.ofNullable(resource);
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

    public void setResource(Resource resource) {
        final var previousResource = this.resource;

        this.resource = resource;

        if (previousResource != null && previousResource != resource) {
            previousResource.removeTopicResource(this);

            if (isPrimary()) {
                previousResource.setRandomPrimaryTopic();
            }
        }

        if (resource != null) {
            if (!resource.getTopicResources().contains(this)) {
                resource.addTopicResource(this);
            }
        }
    }

    @PreRemove
    public void preRemove() {
        this.setTopic(null);
        this.setResource(null);
    }
}
