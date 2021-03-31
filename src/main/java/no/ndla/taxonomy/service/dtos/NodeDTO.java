package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.service.MetadataIdField;

import java.net.URI;
import java.util.Set;

@ApiModel("Topic")
public class NodeDTO {
    @JsonProperty
    @ApiModelProperty(value = "Node id", example = "urn:topic:234")
    @MetadataIdField
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "Node type", example = "urn:nodetype:topic")
    private URI nodeType;

    @JsonProperty
    @ApiModelProperty(value = "The name of the node", example = "Trigonometry")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The primary path for this node", example = "/subject:1/topic:1")
    private String path;

    @JsonProperty
    @ApiModelProperty(value = "List of all paths to this node")
    private Set<String> paths;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public NodeDTO() {

    }

    public NodeDTO(Topic topic, String languageCode) {
        this.id = topic.getPublicId();
        this.nodeType = topic.getNodeType().map(NodeType::getPublicId).orElse(null);
        this.contentUri = topic.getContentUri();
        this.paths = topic.getAllPaths();

        this.path = topic.getPrimaryPath()
                .orElse(null);

        this.name = topic.getTranslation(languageCode)
                .map(TopicTranslation::getName)
                .orElse(topic.getName());
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public URI getNodeType() {
        return nodeType;
    }

    public String getPath() {
        return path;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    protected void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }
}
