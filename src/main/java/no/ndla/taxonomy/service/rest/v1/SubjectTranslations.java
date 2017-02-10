package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectTranslation;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = "/v1/subjects/{id}/translations")
@Transactional
public class SubjectTranslations {
    private SubjectRepository subjectRepository;

    private EntityManager entityManager;

    public SubjectTranslations(SubjectRepository subjectRepository, EntityManager entityManager) {
        this.subjectRepository = subjectRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    @ApiOperation("Gets all translations for a single subject")
    public List<SubjectTranslationIndexDocument> index(@PathVariable("id") URI id) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        List<SubjectTranslationIndexDocument> result = new ArrayList<>();
        subject.getTranslations().forEachRemaining(t -> result.add(
                new SubjectTranslationIndexDocument() {{
                    name = t.getName();
                    language = t.getLanguageCode();
                }})
        );
        return result;
    }

    @GetMapping("/{language}")
    @ApiOperation("Gets a single translation for a single subject")
    public SubjectTranslationIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        SubjectTranslation translation = subject.getTranslation(language);
        if (translation == null)
            throw new NotFoundException("translation with language code " + language + " for subject", id);
        return new SubjectTranslationIndexDocument() {{
            name = translation.getName();
            language = translation.getLanguageCode();
        }};
    }

    @DeleteMapping("/{language}")
    @ApiOperation("Deletes a translation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language
    ) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        SubjectTranslation translation = subject.getTranslation(language);
        if (translation == null) return;
        subject.removeTranslation(language);
        entityManager.remove(translation);
    }

    @PutMapping("/{language}")
    @ApiOperation("Creates or updates a translation of a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb", required = true)
            @PathVariable("language") String language,
            @ApiParam(name = "subject", value = "The new or updated translation")
            @RequestBody UpdateSubjectTranslationCommand command
    ) throws Exception {
        Subject subject = subjectRepository.getByPublicId(id);
        SubjectTranslation translation = subject.addTranslation(language);
        entityManager.persist(translation);
        translation.setName(command.name);
    }

    public static class SubjectTranslationIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the subject", example = "Mathematics")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "ISO 639-1 language code", example = "en")
        public String language;
    }

    public static class UpdateSubjectTranslationCommand {
        @JsonProperty
        @ApiModelProperty(value = "The translated name of the subject", example = "Mathematics")
        public String name;
    }

}
