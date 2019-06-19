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
        this.topic = topic;
        this.resourceType = resourceType;
        setPublicId(URI.create("urn:topic-resourcetype:" + UUID.randomUUID()));
    }

    public Topic getTopic() {
        return topic;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }
}
