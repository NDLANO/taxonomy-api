package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import no.ndla.taxonomy.domain.DomainObject;
import no.ndla.taxonomy.domain.exceptions.DuplicateIdException;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateCommand;
import no.ndla.taxonomy.service.URNValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Transactional
public abstract class CrudController<T extends DomainObject> {
    protected TaxonomyRepository<T> repository;

    private static final Map<Class<?>, String> locations = new HashMap<>();
    private final URNValidator validator = new URNValidator();

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        repository.delete(repository.getByPublicId(id));
        repository.flush();
    }

    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    protected T doPut(URI id, UpdateCommand<T> command) {
        T entity = repository.getByPublicId(id);
        validator.validate(id, entity);
        command.apply(entity);
        return entity;
    }

    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    protected ResponseEntity<Void> doPost(T entity, CreateCommand<T> command) {
        try {
            if (null != command.getId()) {
                validator.validate(command.getId(), entity);
                entity.setPublicId(command.getId());
            }
            command.apply(entity);
            URI location = URI.create(getLocation() + "/" + entity.getPublicId());
            repository.saveAndFlush(entity);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            if (command.getId() != null) {
                throw new DuplicateIdException(command.getId().toString());
            }

            throw new DuplicateIdException();
        }
    }

    protected String getLocation() {
        return locations.computeIfAbsent(getClass(), aClass -> aClass.getAnnotation(RequestMapping.class).path()[0]);
    }
}
