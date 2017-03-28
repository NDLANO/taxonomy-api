package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.ResourceType;
import no.ndla.taxonomy.service.domain.ResourceTypeTranslation;
import no.ndla.taxonomy.service.repositories.ResourceTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"resource-types/{id}/translations", "/v1/resource-types/{id}/translations"})
@Transactional
public class ResourceTypeTranslations {

    private ResourceTypeRepository resourceTypeRepository;

    private EntityManager entityManager;

    public ResourceTypeTranslations(ResourceTypeRepository resourceTypeRepository, EntityManager entityManager) {
        this.resourceTypeRepository = resourceTypeRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all translations for a single resource type")
    public List<ResourceTypeTranslations.ResourceTypeTranslationIndexDocument> index(@PathVariable("id") URI id) throws Exception {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        List<ResourceTypeTranslations.ResourceTypeTranslationIndexDocument> result = new ArrayList<>();
        resourceType.getTranslations().forEachRemaining(t -> result.add(
                new ResourceTypeTranslations.ResourceTypeTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single resource type")
    public ResourceTypeTranslations.ResourceTypeTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        ResourceTypeTranslation translation = resourceType.getTranslation(language);
        if (translation == null)
            throw new NotFoundException("translation with language code " + language + " for resource type", id);
        return new ResourceTypeTranslations.ResourceTypeTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a resource type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "resourceType", value = "The new or updated translation")
            @RequestBody ResourceTypeTranslations.UpdateResourceTypeTranslationCommand command
    ) throws Exception {
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        ResourceTypeTranslation translation = resourceType.addTranslation(language);
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
        ResourceType resourceType = resourceTypeRepository.getByPublicId(id);
        ResourceTypeTranslation translation = resourceType.getTranslation(language);
        if (translation == null) return;
        resourceType.removeTranslation(language);
        entityManager.remove(translation);
    }

    public static class ResourceTypeTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource type", example = "Article")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateResourceTypeTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the resource type", example = "Article")
        public String name;
    }
}
