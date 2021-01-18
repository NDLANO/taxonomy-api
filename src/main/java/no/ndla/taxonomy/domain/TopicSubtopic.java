package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicSubtopic extends DomainEntity implements EntityWithPathConnection {

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "subtopic_id")
    private Topic subtopic;

    @Column(name = "rank")
    private int rank;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private TopicSubtopic() {
        setPublicId(URI.create("urn:topic-subtopic:" + UUID.randomUUID()));
    }

    public static TopicSubtopic create(Topic parentTopic, Topic subTopic) {
        if (parentTopic == null || subTopic == null) {
            throw new NullPointerException();
        }

        final var topicSubtopic = new TopicSubtopic();

        topicSubtopic.topic = parentTopic;
        topicSubtopic.subtopic = subTopic;

        parentTopic.addChildTopicSubtopic(topicSubtopic);
        subTopic.addParentTopicSubtopic(topicSubtopic);

        return topicSubtopic;
    }

    public void disassociate() {
        final var subTopic = this.subtopic;
        final var topic = this.topic;

        this.subtopic = null;
        this.topic = null;

        if (topic != null) {
            topic.removeChildTopicSubTopic(this);
        }

        if (subTopic != null) {
            subTopic.removeParentTopicSubtopic(this);
        }
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public Optional<Topic> getSubtopic() {
        return Optional.ofNullable(subtopic);
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public Optional<EntityWithPath> getConnectedParent() {
        return Optional.ofNullable(topic);
    }

    @Override
    public Optional<EntityWithPath> getConnectedChild() {
        return Optional.ofNullable(subtopic);
    }

    @PreRemove
    public void preRemove() {
        disassociate();
    }

    @Override
    public Optional<Boolean> isPrimary() {
        return Optional.empty();
    }

    @Override
    public void setPrimary(boolean isPrimary) {
        if (isPrimary) {
            return;
        }

        throw new UnsupportedOperationException("TopicSubtopic can not be non-primary");
    }

    @Override
    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }
    
    @Override
    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }
}
