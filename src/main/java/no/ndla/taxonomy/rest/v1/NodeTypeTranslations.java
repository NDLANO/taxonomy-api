package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.NodeTypeTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.NodeTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/nodetypes/{id}/translations"})
@Transactional
public class NodeTypeTranslations {

    private final NodeTypeRepository nodeTypeRepository;

    private final EntityManager entityManager;

    public NodeTypeTranslations(NodeTypeRepository nodeTypeRepository, EntityManager entityManager) {
        this.nodeTypeRepository = nodeTypeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all translations for a single node type")
    public List<NodeTypeTranslationIndexDocument> index(@PathVariable("id") URI id) {
        NodeType nodeType = nodeTypeRepository.getByPublicId(id);
        List<NodeTypeTranslationIndexDocument> result = new ArrayList<>();
        nodeType.getTranslations().forEach(t -> result.add(
                new NodeTypeTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single node type")
    public NodeTypeTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) {
        NodeType nodeType = nodeTypeRepository.getByPublicId(id);
        NodeTypeTranslation translation = nodeType.getTranslation(language).orElseThrow(() -> new NotFoundException("translation with language code " + language + " for node type", id));
        return new NodeTypeTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a node type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "nodeType", value = "The new or updated translation")
            @RequestBody UpdateNodeTypeTranslationCommand command
    ) {
        NodeType nodeType = nodeTypeRepository.getByPublicId(id);
        NodeTypeTranslation translation = nodeType.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) {
        NodeType nodeType = nodeTypeRepository.getByPublicId(id);
        nodeType.getTranslation(language).ifPresent(nodeTypeTranslation -> {
            nodeType.removeTranslation(language);
            entityManager.remove(nodeTypeTranslation);
        });
    }

    public static class NodeTypeTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the node type", example = "Program area")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateNodeTypeTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the node type", example = "Program area")
        public String name;
    }
}
