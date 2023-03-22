/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.NodeConnection;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/** */
@Schema(name = "Connections")
public class NodeConnectionDTO {

    @JsonProperty
    @Schema(description = "The id of the subject-topic or topic-subtopic connection", example = "urn:subject-topic:1")
    private URI connectionId;

    @JsonProperty
    @Schema(description = "The id of the connected subject or topic", example = "urn:subject:1")
    private URI targetId;

    @JsonProperty
    @Schema(description = "The path part of the url for the subject or subtopic connected to this topic", example = "/subject:1/topic:1")
    private Set<String> paths = new HashSet<>();

    @JsonProperty
    @Schema(description = "The type of connection (parent subject, parent topic or subtopic")
    private String type;

    @JsonProperty
    @Schema(description = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    private Boolean isPrimary;

    public NodeConnectionDTO() {
    }

    private NodeConnectionDTO(NodeConnection connection, boolean isParentConnection) {
        this.connectionId = connection.getPublicId();
        this.isPrimary = true;

        final var connectedObject = isParentConnection ? connection.getParent()
                : connection.getChild();

        connectedObject.ifPresent(connected -> {
            this.targetId = connected.getPublicId();
            this.paths = Set.copyOf(connected.getAllPaths());
        });

        if (isParentConnection) {
            this.type = "parent-topic";
        } else {
            this.type = "subtopic";
        }
    }

    public static NodeConnectionDTO parentConnection(NodeConnection connection) {
        return new NodeConnectionDTO(connection, true);
    }

    public static NodeConnectionDTO childConnection(NodeConnection connection) {
        return new NodeConnectionDTO(connection, false);
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public URI getTargetId() {
        return targetId;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public String getType() {
        return type;
    }

    public Boolean getPrimary() {
        return isPrimary;
    }
}
