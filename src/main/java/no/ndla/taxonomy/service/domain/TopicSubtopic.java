package no.ndla.taxonomy.service.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicSubtopic extends DomainEntity {

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "subtopic_id")
    private Topic subtopic;

    @Column(name = "is_primary")
    private boolean primary;

    protected TopicSubtopic() {
    }

    public TopicSubtopic(Topic topic, Topic subtopic) {
        this.topic = topic;
        this.subtopic = subtopic;
        setPublicId(URI.create("urn:topic-subtopic:" + UUID.randomUUID()));
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

    public Topic getSubtopic() {
        return subtopic;
    }
}
