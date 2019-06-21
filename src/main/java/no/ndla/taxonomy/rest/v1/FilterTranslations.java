package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.FilterTranslation;
import no.ndla.taxonomy.domain.NotFoundException;
import no.ndla.taxonomy.repositories.FilterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/filters/{id}/translations"})
@Transactional
public class FilterTranslations {

    private FilterRepository filterRepository;

    private EntityManager entityManager;

    public FilterTranslations(FilterRepository filterRepository, EntityManager entityManager) {
        this.filterRepository = filterRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all relevanceTranslations for a single filter")
    public List<FilterTranslations.FilterTranslationIndexDocument> index(@PathVariable("id") URI id) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        List<FilterTranslations.FilterTranslationIndexDocument> result = new ArrayList<>();
        filter.getTranslations().forEachRemaining(t -> result.add(
                new FilterTranslations.FilterTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single filter")
    public FilterTranslations.FilterTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        FilterTranslation translation = filter.getTranslation(language).orElseThrow(() -> new NotFoundException("Translation with language code " + language + " for filter", id));

        return new FilterTranslations.FilterTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "filter", value = "The new or updated translation")
            @RequestBody FilterTranslations.UpdateFilterTranslationCommand command
    ) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        FilterTranslation translation = filter.addTranslation(language);
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
    ) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        filter.getTranslation(language).ifPresent(translation -> {
            filter.removeTranslation(language);
            entityManager.remove(translation);
        });
    }


    @ApiModel("FilterTranslationIndexDocument")
    public static class FilterTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the filter", example = "Carpenter")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }


    public static class UpdateFilterTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the filter", example = "Carpenter")
        public String name;
    }
}
