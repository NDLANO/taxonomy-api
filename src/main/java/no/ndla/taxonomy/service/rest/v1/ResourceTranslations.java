package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.ResourceTranslation;
import no.ndla.taxonomy.service.repositories.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/resources/{id}/translations", "/v1/resources/{id}/translations"})
@Transactional
public class ResourceTranslations {

    private ResourceRepository resourceRepository;

    private EntityManager entityManager;

    public ResourceTranslations(ResourceRepository resourceRepository, EntityManager entityManager) {
        this.resourceRepository = resourceRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all translations for a single resource")
    public List<ResourceTranslations.ResourceTranslationIndexDocument> index(@PathVariable("id") URI id) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        List<ResourceTranslations.ResourceTranslationIndexDocument> result = new ArrayList<>();
        resource.getTranslations().forEachRemaining(t -> result.add(
                new ResourceTranslations.ResourceTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single resource")
    public ResourceTranslations.ResourceTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        ResourceTranslation translation = resource.getTranslation(language);
        if (translation == null)
            throw new NotFoundException("translation with language code " + language + " for resource", id);
        return new ResourceTranslations.ResourceTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "resource", value = "The new or updated translation")
            @RequestBody ResourceTranslations.UpdateResourceTranslationCommand command
    ) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        ResourceTranslation translation = resource.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        Resource resource = resourceRepository.getByPublicId(id);
        ResourceTranslation translation = resource.getTranslation(language);
        if (translation == null) return;
        resource.removeTranslation(language);
        entityManager.remove(translation);
    }

    public static class ResourceTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource", example = "Introduction to algebra")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateResourceTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource", example = "Introduction to algebra")
        public String name;
    }
}
