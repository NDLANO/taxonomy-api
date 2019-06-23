package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "topic_tree_by_topic_id_view")
public class TopicTreeByTopicElement {
    @Id
    private String id;

    private int rootTopicId;

    private int topicId;

    private int parentTopicId;

    private int topicRank;

    private int topicLevel;

    public String getId() {
        return id;
    }

    public int getRootTopicId() {
        return rootTopicId;
    }

    public int getTopicId() {
        return topicId;
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
}
