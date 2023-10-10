/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.UpdatableDto;

public class NodePostPut implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(
            description =
                    "If specified, set the node_id to this value. If omitted, an uuid will be assigned automatically.")
    public Optional<String> nodeId = Optional.empty();

    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Schema(description = "Type of node.", example = "topic")
    public NodeType nodeType;

    @JsonProperty
    @Schema(
            description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.",
            example = "urn:article:1")
    public Optional<URI> contentUri = Optional.empty();

    @JsonProperty
    @Schema(description = "The name of the node. Required on create.", example = "Trigonometry")
    public Optional<String> name = Optional.empty();

    @JsonProperty
    @Deprecated
    @Schema(description = "The node is a root node. Default is false. Only used if present.")
    public Optional<Boolean> root = Optional.empty();

    @JsonProperty
    @Schema(description = "The node is the root in a context. Default is false. Only used if present.")
    public Optional<Boolean> context = Optional.empty();

    @Schema(description = "The node is visible. Default is true.")
    public Optional<Boolean> visible = Optional.empty();

    public Optional<String> getNodeId() {
        return nodeId;
    }

    @JsonIgnore
    public URI getPublicId() {
        return URI.create("urn:" + nodeType.getName() + ":" + nodeId.get());
    }

    @Override
    public void apply(Node node) {
        if (node.getIdent() == null) {
            node.setIdent(UUID.randomUUID().toString());
        }
        if (getNodeId().isPresent()) {
            node.setPublicId(getPublicId());
        }
        if (nodeType != null) {
            node.setNodeType(nodeType);
        }
        root.ifPresent(node::setContext);
        context.ifPresent(node::setContext);
        name.ifPresent(node::setName);
        contentUri.ifPresent(node::setContentUri);
        visible.ifPresent(node::setVisible);
    }
}
