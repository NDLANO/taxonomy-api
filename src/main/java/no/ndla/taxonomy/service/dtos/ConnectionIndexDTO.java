package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.EntityWithPathConnection;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.TopicSubtopic;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@ApiModel("Connections")
public class ConnectionIndexDTO {

    @JsonProperty
    @ApiModelProperty(value = "The id of the subject-topic or topic-subtopic connection", example = "urn:subject-topic:1")
    public URI connectionId;
    @JsonProperty
    @ApiModelProperty(value = "The id of the connected subject or topic", example = "urn:subject:1")
    public URI targetId;
    @JsonProperty
    @ApiModelProperty(value = "The path part of the url for the subject or subtopic connected to this topic", example = "/subject:1/topic:1")
    public Set<String> paths = new HashSet<>();
    @JsonProperty
    @ApiModelProperty(value = "The type of connection (parent subject, parent topic or subtopic")
    public String type;
    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    public Boolean isPrimary;

    public ConnectionIndexDTO() {
    }

    private ConnectionIndexDTO(EntityWithPathConnection connection, boolean isParentConnection) {
        this.connectionId = connection.getPublicId();
        this.isPrimary = connection.isPrimary();

        final var connectedObject = isParentConnection ? connection.getConnectedParent() : connection.getConnectedChild();

        connectedObject.ifPresent(connected -> {
            this.targetId = connected.getPublicId();
            this.paths = Set.copyOf(connected.getAllPaths());
        });

        if (connection instanceof TopicSubtopic) {
            if (isParentConnection) {
                this.type = "parent-topic";
            } else {
                this.type = "subtopic";
            }
        } else if (connection instanceof SubjectTopic) {
            if (isParentConnection) {
                this.type = "parent-subject";
            } else {
                this.type = "topic";
            }
        }
    }

    public static ConnectionIndexDTO parentConnection(EntityWithPathConnection connection) {
        return new ConnectionIndexDTO(connection, true);
    }

    public static ConnectionIndexDTO childConnection(EntityWithPathConnection connection) {
        return new ConnectionIndexDTO(connection, false);
    }
}
