package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "topic_tree_by_subject_id_view")
public class SubjectTopicTreeElement {
    @Id
    private int topicId;

    private int connectionId;

    private int parentTopicId;

    private int topicRank;

    private int topicLevel;

    private int subjectId;

    public int getTopicId() {
        return topicId;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public int getParentTopicId() {
        return parentTopicId;
    }

    public int getTopicRank() {
        return topicRank;
    }

    public int getTopicLevel() {
        return topicLevel;
    }

    public int getSubjectId() {
        return subjectId;
    }
}
