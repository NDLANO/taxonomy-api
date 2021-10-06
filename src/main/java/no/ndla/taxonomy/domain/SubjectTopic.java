/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class SubjectTopic extends DomainEntity implements EntityWithPathConnection {

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "rank")
    private int rank;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private SubjectTopic() {
        setPublicId(URI.create("urn:subject-topic:" + UUID.randomUUID()));
    }

    public static SubjectTopic create(Subject subject, Topic topic) {
        if (topic == null || subject == null) {
            throw new NullPointerException();
        }

        final var subjectTopic = new SubjectTopic();
        subjectTopic.subject = subject;
        subjectTopic.topic = topic;

        subject.addSubjectTopic(subjectTopic);
        topic.addSubjectTopic(subjectTopic);

        return subjectTopic;
    }

    public void disassociate() {
        final var subject = this.subject;
        final var topic = this.topic;

        this.subject = null;
        this.topic = null;

        if (subject != null) {
            subject.removeSubjectTopic(this);
        }

        if (topic != null) {
            topic.removeSubjectTopic(this);
        }
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @PreRemove
    void preRemove() {
        disassociate();
    }

    @Override
    public Optional<EntityWithPath> getConnectedParent() {
        return Optional.ofNullable(subject);
    }

    @Override
    public Optional<EntityWithPath> getConnectedChild() {
        return Optional.ofNullable(topic);
    }

    @Override
    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    @Override
    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
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

        throw new UnsupportedOperationException("SubjectTopic can not be non-primary");
    }
}
