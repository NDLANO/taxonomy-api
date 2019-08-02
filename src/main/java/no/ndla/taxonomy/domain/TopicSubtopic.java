package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicSubtopic extends DomainEntity implements Rankable{

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "subtopic_id")
    private Topic subtopic;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    public TopicSubtopic() {
        setPublicId(URI.create("urn:topic-subtopic:" + UUID.randomUUID()));
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public void setTopic(Topic topic) {
        final var previousTopic = this.topic;

        this.topic = topic;

        if (this.topic != previousTopic && previousTopic != null) {
            previousTopic.removeChildTopicSubTopic(this);
        }

        if (topic != null && !topic.getChildrenTopicSubtopics().contains(this)) {
            topic.addChildTopicSubtopic(this);
        }
    }

    public Optional<Topic> getSubtopic() {
        return Optional.ofNullable(subtopic);
    }

    public void setSubtopic(Topic subtopic) {
        final var previousSubtopic = this.subtopic;

        this.subtopic = subtopic;

        if (this.subtopic != previousSubtopic && previousSubtopic != null) {
            previousSubtopic.removeParentTopicSubtopic(this);
            if (isPrimary()) {
                previousSubtopic.setRandomPrimaryTopic();
            }
        }

        if (subtopic != null && !subtopic.getParentTopicSubtopics().contains(this)) {
            subtopic.addParentTopicSubtopic(this);
        }
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @PreRemove
    public void preRemove() {
        this.setTopic(null);
        this.setSubtopic(null);
    }
}
