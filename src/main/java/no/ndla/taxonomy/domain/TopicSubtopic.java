package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicSubtopic extends DomainEntity implements Rankable{

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "subtopic_id")
    private Topic subtopic;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    protected TopicSubtopic() {
    }

    public TopicSubtopic(Topic topic, Topic subtopic) {
        setPublicId(URI.create("urn:topic-subtopic:" + UUID.randomUUID()));

        this.setTopic(topic);
        this.setSubtopic(subtopic);
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

    public void setTopic(Topic topic) {
        this.topic = topic;

        if (topic != null && !topic.getChildrenTopicSubtopics().contains(this)) {
            topic.addChildTopicSubtopic(this);
        }
    }

    public void setSubtopic(Topic subtopic) {
        this.subtopic = subtopic;

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
}
