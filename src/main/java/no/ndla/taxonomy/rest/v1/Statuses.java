package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Status;
import no.ndla.taxonomy.domain.StatusTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.StatusRepository;
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
@RequestMapping(path = {"/v1/statuses"})
@Transactional
public class Statuses extends CrudController<Status> {
    private final StatusRepository statusRepository;

    public Statuses(StatusRepository statusRepository) {
        super(statusRepository);

        this.statusRepository = statusRepository;
    }

    @GetMapping
    @ApiOperation("Gets a list of all statuses")
    public List<StatusIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return statusRepository.findAllIncludingTranslations()
                .stream()
                .map(status -> new StatusIndexDocument(status, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single status")
    public StatusIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return statusRepository.findFirstByPublicIdIncludingTranslations(id)
                .map(status -> new StatusIndexDocument(status, language))
                .orElseThrow(() -> new NotFoundException("Status", id));
    }

    @PostMapping
    @ApiOperation(value = "Adds a new status")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "status", value = "The new status")
            @RequestBody StatusCommand command
    ) {
        Status status = new Status();
        return doPost(status, command);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a status. You can also update the id, take care!")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable URI id,
            @ApiParam(name = "status", value = "The updated status. Fields not included will be set to null.")
            @RequestBody StatusCommand
                    command
    ) {
        Status status = doPut(id, command);

        if (command.id != null) {
            status.setPublicId(command.id);
        }
    }


    @ApiModel("StatusIndexDocument")
    public static class StatusIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:status:draft")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the status", example = "Draft")
        public String name;

        public StatusIndexDocument() {

        }

        public StatusIndexDocument(Status status, String language) {
            this.id = status.getPublicId();
            this.name = status.getTranslation(language)
                    .map(StatusTranslation::getName)
                    .orElse(status.getName());
        }
    }

    public static class StatusCommand implements UpdatableDto<Status> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:status: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:status:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the status", example = "Draft")
        public String name;

        @Override
        public Optional<URI> getId() {
            return Optional.ofNullable(id);
        }

        @Override
        public void apply(Status entity) {
            entity.setName(name);
        }
    }
}
