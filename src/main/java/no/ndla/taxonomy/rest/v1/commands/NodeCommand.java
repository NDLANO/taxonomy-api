package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.UpdatableDto;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.net.URI;
import java.util.Optional;

public class NodeCommand implements UpdatableDto<Node> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the node_id to this value. If omitted, an uuid will be assigned automatically.")
    public String nodeId;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    @ApiModelProperty(required = true, value = "Type of node. Values are subject, topic", example = "topic")
    public NodeType nodeType;

    @JsonProperty
    @ApiModelProperty(value = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the node", example = "Trigonometry")
    public String name;

    public Optional<String> getNodeId() {
        return Optional.ofNullable(nodeId);
    }

    @Override
    public void apply(Node node) {
        if(getNodeId().isPresent())
            node.setPublicId(URI.create("urn:" + nodeType.getName() + ":" + getNodeId()));
        node.setIdent(nodeId);
        node.setNodeType(nodeType);
        node.setName(name);
        node.setContentUri(contentUri);
    }
}