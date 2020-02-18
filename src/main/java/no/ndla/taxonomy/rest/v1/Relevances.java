package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.RelevanceTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateCommand;
import no.ndla.taxonomy.service.MetadataApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/relevances"})
@Transactional
public class Relevances extends CrudController<Relevance> {

    private final RelevanceRepository relevanceRepository;

    public Relevances(RelevanceRepository repository, MetadataApiService metadataApiService) {
        super(metadataApiService);

        this.repository = relevanceRepository = repository;
    }

    @GetMapping
    @ApiOperation("Gets all relevances")
    public List<RelevanceIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return relevanceRepository.findAllIncludingTranslations()
                .stream()
                .map(relevance -> new RelevanceIndexDocument(relevance, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single relevance", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public RelevanceIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return relevanceRepository.findFirstByPublicIdIncludingTranslations(id)
                .map(relevance -> new RelevanceIndexDocument(relevance, language))
                .orElseThrow(() -> new NotFoundException("Relevance", id));
    }

    @PostMapping
    @ApiOperation(value = "Creates a new relevance")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "relevance", value = "The new relevance") @RequestBody CreateRelevanceCommand command) {
        return doPost(new Relevance(), command);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a relevance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "relevance", value = "The updated relevance. Fields not included will be set to null.") @RequestBody UpdateRelevanceCommand command
    ) {
        doPut(id, command);
    }

    @ApiModel("RelevanceIndexDocument")
    public static class RelevanceIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:relevance:core")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the relevance", example = "Core")
        public String name;

        public RelevanceIndexDocument() {

        }

        public RelevanceIndexDocument(Relevance relevance, String language) {
            this.id = relevance.getPublicId();
            this.name = relevance.getTranslation(language)
                    .map(RelevanceTranslation::getName)
                    .orElse(relevance.getName());
        }
    }

    public static class CreateRelevanceCommand extends CreateCommand<Relevance> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:relevance: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:relevance:supplementary")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the relevance", example = "Supplementary")
        public String name;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Relevance entity) {
            entity.setName(name);
        }
    }

    public static class UpdateRelevanceCommand extends UpdateCommand<Relevance> {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the relevance", example = "Supplementary")
        public String name;

        @Override
        public void apply(Relevance entity) {
            entity.setName(name);
        }
    }
}
