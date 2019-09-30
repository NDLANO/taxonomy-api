package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicResource extends DomainEntity implements EntityWithPathConnection {

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "rank")
    private int rank;

    private TopicResource() {
        setPublicId(URI.create("urn:topic-resource:" + UUID.randomUUID()));
    }

    public static TopicResource create(Topic topic, Resource resource) {
        final var topicResource = new TopicResource();

        topicResource.topic = topic;
        topicResource.resource = resource;

        topic.addTopicResource(topicResource);
        resource.addTopicResource(topicResource);

        return topicResource;
    }

    public void disassociate() {
        final var topic = this.topic;
        final var resource = this.resource;

        this.topic = null;
        this.resource = null;

        if (topic != null) {
            topic.removeTopicResource(this);
        }

        if (resource != null) {
            resource.removeTopicResource(this);
        }
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public Optional<Resource> getResource() {
        return Optional.ofNullable(resource);
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public Optional<EntityWithPath> getConnectedParent() {
        return Optional.ofNullable(topic);
    }

    @Override
    public Optional<EntityWithPath> getConnectedChild() {
        return Optional.ofNullable(resource);
    }

    @PreRemove
    public void preRemove() {
        disassociate();
    }
}
