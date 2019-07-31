package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicResourceType extends DomainEntity {
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    public TopicResourceType() {
        setPublicId(URI.create("urn:topic-resourcetype:" + UUID.randomUUID()));
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public void setTopic(Topic topic) {
        final var previousTopic = this.topic;

        this.topic = topic;

        if (previousTopic != null && previousTopic != topic) {
            previousTopic.removeTopicResourceType(this);
        }

        if (topic != null && !topic.getTopicResourceTypes().contains(this)) {
            topic.addTopicResourceType(this);
        }
    }

    public Optional<ResourceType> getResourceType() {
        return Optional.ofNullable(resourceType);
    }

    public void setResourceType(ResourceType resourceType) {
        final var previousResourceType = this.resourceType;

        this.resourceType = resourceType;

        if (previousResourceType != null && previousResourceType != resourceType) {
            previousResourceType.removeTopicResourceType(this);
        }

        if (resourceType != null && !resourceType.getTopicResourceTypes().contains(this)) {
            resourceType.addTopicResourceType(this);
        }
    }
}
