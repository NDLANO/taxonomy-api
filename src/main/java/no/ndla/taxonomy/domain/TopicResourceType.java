package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicResourceType extends DomainEntity {
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    private TopicResourceType() {
        setPublicId(URI.create("urn:topic-resourcetype:" + UUID.randomUUID()));
    }

    public static TopicResourceType create(Topic topic, ResourceType resourceType) {
        if (topic == null || resourceType == null) {
            throw new NullPointerException();
        }

        final var topicResourceType = new TopicResourceType();

        topicResourceType.topic = topic;
        topicResourceType.resourceType = resourceType;

        resourceType.addTopicResourceType(topicResourceType);
        topic.addTopicResourceType(topicResourceType);

        return topicResourceType;
    }

    public void disassociate() {
        final var topic = this.topic;
        final var resourceType = this.resourceType;

        this.topic = null;
        this.resourceType = null;

        if (topic != null) {
            topic.removeTopicResourceType(this);
        }

        if (resourceType != null) {
            resourceType.removeTopicResourceType(this);
        }
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public Optional<ResourceType> getResourceType() {
        return Optional.ofNullable(resourceType);
    }

    @PreRemove
    void preRemove() {
        disassociate();
    }
}
