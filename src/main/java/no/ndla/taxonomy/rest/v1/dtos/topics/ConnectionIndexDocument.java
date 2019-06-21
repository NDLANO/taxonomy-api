package no.ndla.taxonomy.rest.v1.dtos.topics;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.TopicSubtopic;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@ApiModel("Connections")
public class ConnectionIndexDocument {

    @JsonProperty
    @ApiModelProperty(value = "The id of the subject-topic or topic-subtopic connection", example = "urn:subject-topic:1")
    public URI connectionId;
    @JsonProperty
    @ApiModelProperty(value = "The id of the connected subject or topic", example = "urn:subject:1")
    public URI targetId;
    @JsonProperty
    @ApiModelProperty(value = "The path part of the url for the subject or subtopic connected to this topic", example = "/subject:1/topic:1")
    public List<String> paths = new ArrayList<>();
    @JsonProperty
    @ApiModelProperty(value = "The type of connection (parent subject, parent topic or subtopic")
    public String type;
    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    public Boolean isPrimary;

    public ConnectionIndexDocument() {
    }

    private ConnectionIndexDocument(TopicSubtopic topicSubtopic, boolean parentTopicConnection) {
        if (parentTopicConnection) {
            this.connectionId = topicSubtopic.getPublicId();
            this.targetId = topicSubtopic.getTopic().getPublicId();
            this.paths = new ArrayList<>(topicSubtopic.getTopic().getAllPaths());
            this.type = "parent-topic";
            this.isPrimary = topicSubtopic.isPrimary();
        } else {
            this.connectionId = topicSubtopic.getPublicId();
            this.targetId = topicSubtopic.getSubtopic().getPublicId();
            this.paths = new ArrayList<>(topicSubtopic.getSubtopic().getAllPaths());
            this.type = "subtopic";
            this.isPrimary = topicSubtopic.isPrimary();
        }
    }

    public ConnectionIndexDocument(SubjectTopic subjectTopic) {
        this.connectionId = subjectTopic.getPublicId();
        this.targetId = subjectTopic.getSubject().getPublicId();
        this.paths = new ArrayList<>(subjectTopic.getSubject().getAllPaths());
        this.type = "parent-subject";
        this.isPrimary = subjectTopic.isPrimary();
    }

    public static ConnectionIndexDocument parentConnection(TopicSubtopic topicSubtopic) {
        return new ConnectionIndexDocument(topicSubtopic, true);
    }

    public static ConnectionIndexDocument subtopicConnection(TopicSubtopic topicSubtopic) {
        return new ConnectionIndexDocument(topicSubtopic, false);
    }
}
