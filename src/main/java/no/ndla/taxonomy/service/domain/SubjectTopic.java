package no.ndla.taxonomy.service.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.UUID;

@Entity
public class SubjectTopic extends DomainEntity {

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "is_primary")
    private boolean primary;

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

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
