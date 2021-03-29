package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.NodeTypeTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeTypeRepository;
import no.ndla.taxonomy.service.UpdatableDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/nodetypes"})
@Transactional
public class NodeTypes extends CrudController<NodeType> {
    private NodeTypeRepository nodeTypeRepository;

    public NodeTypes(NodeTypeRepository nodeTypeRepository) {
        super(nodeTypeRepository);
        this.nodeTypeRepository = nodeTypeRepository;
    }

    @GetMapping
    @ApiOperation("Gets all node types")
    public List<NodeTypeIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return nodeTypeRepository.findAllIncludingTranslations()
                .stream()
                .map(nodeType -> new NodeTypeIndexDocument(nodeType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a node type", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public NodeTypeIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return nodeTypeRepository.findFirstByPublicIdIncludingTranslations(id)
                .map(nodeType -> new NodeTypeIndexDocument(nodeType, language))
                .orElseThrow(() -> new NotFoundException("Node type", id));
    }

    @PostMapping
    @ApiOperation(value = "Creates a new node type")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "relevance", value = "The new relevance") @RequestBody NodeTypeCommand command) {
        return doPost(new NodeType(), command);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a node type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "nodetype", value = "The updated node type. Fields not included will be set to null.") @RequestBody NodeTypeCommand command
    ) {
        doPut(id, command);
    }

    @ApiModel("NodeTypeIndexDocument")
    public static class NodeTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:nodetype:educationprogram")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the node type", example = "Education program")
        public String name;

        public NodeTypeIndexDocument() {

        }

        public NodeTypeIndexDocument(NodeType nodeType, String language) {
            this.id = nodeType.getPublicId();
            this.name = nodeType.getTranslation(language)
                    .map(NodeTypeTranslation::getName)
                    .orElse(nodeType.getName());
        }
    }

    public static class NodeTypeCommand implements UpdatableDto<NodeType> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:nodetype: and be a valid URI. If ommitted, an id will be assigned automatically. Ignored on update", example = "urn:nodetype:educationprogram")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the node type", example = "Education program")
        public String name;

        @Override
        public Optional<URI> getId() {
            return Optional.ofNullable(id);
        }

        @Override
        public void apply(NodeType entity) {
            entity.setName(name);
        }
    }
}
