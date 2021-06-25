package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Status;
import no.ndla.taxonomy.domain.StatusTranslation;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.StatusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping(path = {"/v1/statuses/{id}/translations"})
@Transactional
public class StatusTranslations {

    private final StatusRepository statusRepository;

    private final EntityManager entityManager;

    public StatusTranslations(StatusRepository statusRepository, EntityManager entityManager) {
        this.statusRepository = statusRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all statusTranslations for a single status")
    public List<StatusTranslationIndexDocument> index(@PathVariable("id") URI id) {
        Status status = statusRepository.getByPublicId(id);
        List<StatusTranslationIndexDocument> result = new ArrayList<>();
        status.getTranslations().forEach(t -> result.add(
                new StatusTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single status")
    public StatusTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) {
        Status status = statusRepository.getByPublicId(id);
        StatusTranslation translation = status.getTranslation(language).orElseThrow(() -> new NotFoundException("translation with language code " + language + " for status", id));

        return new StatusTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "status", value = "The new or updated translation")
            @RequestBody UpdateStatusTranslationCommand command
    ) {
        Status status = statusRepository.getByPublicId(id);
        StatusTranslation translation = status.addTranslation(language);
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
        Status status = statusRepository.getByPublicId(id);
        status.getTranslation(language).ifPresent((translation) -> {
            status.removeTranslation(language);
            entityManager.remove(translation);
        });
    }

    public static class StatusTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the status", example = "Divided")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateStatusTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the status", example = "Divided")
        public String name;
    }
}
