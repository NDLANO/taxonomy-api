package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class SubjectTopic extends DomainEntity implements Rankable {

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    protected SubjectTopic() {

    }

    public SubjectTopic(Subject subject, Topic topic) {
        setPublicId(URI.create("urn:subject-topic:" + UUID.randomUUID()));

        this.setSubject(subject);
        this.setTopic(topic);
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    public void setSubject(Subject subject) {
        final var previousSubject = this.subject;

        this.subject = subject;

        if (subject != previousSubject && previousSubject != null) {
            previousSubject.removeSubjectTopic(this);
        }

        if (subject != null && !subject.getSubjectTopics().contains(this)) {
            subject.addSubjectTopic(this);
        }
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public void setTopic(Topic topic) {
        final var previousTopic = this.topic;

        this.topic = topic;

        if (topic != previousTopic && previousTopic != null) {
            previousTopic.removeSubjectTopic(this);

            if (isPrimary()) {
                previousTopic.setRandomPrimarySubject();
            }
        }

        if (topic != null && !topic.getSubjectTopics().contains(this)) {
            topic.addSubjectTopic(this);
        }
    }

    public boolean isPrimary() {
        return primary;
    }

    void setPrimary(boolean primary) {
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
        this.setTopic(null);
        this.setSubject(null);
    }
}
