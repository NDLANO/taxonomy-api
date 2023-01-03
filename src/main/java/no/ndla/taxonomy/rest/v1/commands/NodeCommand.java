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
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.UpdatableDto;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class NodeCommand implements UpdatableDto<Node> {
    @JsonProperty
    @Schema(description = "If specified, set the node_id to this value. If omitted, an uuid will be assigned automatically.")
    public String nodeId;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Schema(description = "Type of node. Values are subject, topic. Required on create.", example = "topic")
    public NodeType nodeType;

    @JsonProperty
    @Schema(description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @Schema(description = "The name of the node. Required on create.", example = "Trigonometry")
    public String name;

    @JsonProperty
    @Schema(description = "The node is a root node. Default is false. Only used if present.")
    public Boolean root;

    public Optional<String> getNodeId() {
        return Optional.ofNullable(nodeId);
    }

    @JsonIgnore
    public URI getPublicId() {
        return URI.create("urn:" + nodeType.getName() + ":" + nodeId);
    }

    @Override
    public void apply(Node node) {
        if (node.getIdent() == null) {
            node.setIdent(UUID.randomUUID().toString());
        }
        if (getNodeId().isPresent()) {
            node.setPublicId(getPublicId());
        }
        if (root != null) {
            node.setRoot(root);
        }
        if (nodeType != null) {
            node.setNodeType(nodeType);
        }
        if (name != null) {
            node.setName(name);
        }
        if (contentUri != null) {
            node.setContentUri(contentUri);
        }
    }
}
