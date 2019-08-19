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

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    private SubjectTopic() {
        setPublicId(URI.create("urn:subject-topic:" + UUID.randomUUID()));
    }

    public static SubjectTopic create(Subject subject, Topic topic) {
        return create(subject, topic, false);
    }

    public static SubjectTopic create(Subject subject, Topic topic, boolean primary) {
        if (topic == null || subject == null) {
            throw new NullPointerException();
        }

        final var subjectTopic = new SubjectTopic();
        subjectTopic.subject = subject;
        subjectTopic.topic = topic;
        subjectTopic.setPrimary(primary);

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

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
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
}
