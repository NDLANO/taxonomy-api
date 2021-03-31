package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.NodeTypeRepository;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

public class NodeCommand implements UpdatableDto<Topic> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. If omitted, an id will be assigned automatically. Ignored on update", example = "urn:topic:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the node", example = "Trigonometry")
    public String name;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The node type", example = "urn:nodetype:topic")
    public URI nodeType;

    private NodeType nodeTypeDomainObject = null;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    public void resolveNodeType(NodeTypeRepository nodeTypeRepository) {
        nodeTypeDomainObject = nodeType != null ? nodeTypeRepository.findByPublicId(nodeType) : null;
    }

    @Override
    public void apply(Topic topic) {
        topic.setName(name);
        topic.setContentUri(contentUri);
        if (nodeType != null && nodeTypeDomainObject != null && nodeType.equals(nodeTypeDomainObject.getPublicId())) {
            topic.setNodeType(nodeTypeDomainObject);
        }
    }
}
