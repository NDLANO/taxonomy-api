package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicResourceType extends DomainEntity {
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    protected TopicResourceType() {
    }

    public TopicResourceType(Topic topic, ResourceType resourceType) {
        setPublicId(URI.create("urn:topic-resourcetype:" + UUID.randomUUID()));

        this.setTopic(topic);
        this.setResourceType(resourceType);
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;

        if (topic != null && !topic.getTopicResourceTypes().contains(this)) {
            topic.addTopicResourceType(this);
        }
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;

        if (resourceType != null && !resourceType.getTopicResourceTypes().contains(this)) {
            resourceType.addTopicResourceType(this);
        }
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}