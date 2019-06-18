package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.UUID;

@Entity
public class SubjectTopic extends DomainEntity implements Rankable {

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "rank")
    private int rank;

    protected SubjectTopic() {

    }

    public SubjectTopic(Subject subject, Topic topic) {
        this.subject = subject;
        this.topic = topic;
        setPublicId(URI.create("urn:subject-topic:" + UUID.randomUUID()));
    }

    public Subject getSubject() {
        return subject;
    }

    public Topic getTopic() {
        return topic;
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

    @PreUpdate
    @PreRemove
    @PrePersist
    void updateObjectsUpdatedAt() {
        if (topic != null) {
            topic.updateUpdatedAt();
        }
        if (subject != null) {
            subject.updateUpdatedAt();
        }
    }
}
