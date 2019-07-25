package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicResource extends DomainEntity implements Rankable {

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    protected TopicResource() {
    }

    public TopicResource(Topic topic, Resource resource) {
        this.setTopic(topic);
        this.setResource(resource);
        setPublicId(URI.create("urn:topic-resource:" + UUID.randomUUID()));
    }

    public void setTopic(Topic topic) {
        if (this.topic != null && this.topic != topic) {
            this.topic.removeTopicResource(this);
        }

        this.topic = topic;

        if (topic != null && !topic.getTopicResources().contains(this)) {
            topic.addTopicResource(this);
        }
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

    public void setResource(Resource resource) {
        if (this.resource != null && this.resource != resource) {
            this.resource.removeTopicResource(this);
        }

        this.resource = resource;

        if (resource != null) {
            if (resource.getTopicResources().contains(this)) {
                resource.removeTopicResource(this);
            }
        }
    }
}
