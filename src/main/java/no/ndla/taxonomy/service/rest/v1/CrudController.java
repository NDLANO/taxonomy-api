package no.ndla.taxonomy.service.rest.v1;

import io.swagger.annotations.ApiOperation;
import no.ndla.taxonomy.service.domain.DomainObject;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.URNValidator;
import no.ndla.taxonomy.service.repositories.TaxonomyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public abstract class CrudController<T extends DomainObject> {
    protected TaxonomyRepository<T> repository;

    private static final Map<Class<?>, String> locations = new HashMap<>();
    private URNValidator validator = new URNValidator();

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        T resource = repository.getByPublicId(id);
        repository.delete(resource);
    }

    protected T doPut(URI id, UpdateCommand<T> command) {
        T entity = repository.getByPublicId(id);
        validator.validate(id, entity);
        command.apply(entity);
        return entity;
    }

    protected ResponseEntity<Void> doPost(T entity, CreateCommand<T> command) {
        try {
            if (null != command.getId()) {
                validator.validate(command.getId(), entity);
                entity.setPublicId(command.getId());
            }
            command.apply(entity);
            URI location = URI.create(getLocation() + "/" + entity.getPublicId());
            repository.save(entity);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException(command.getId().toString());
        }
    }

    protected String getLocation() {
        return locations.computeIfAbsent(getClass(), aClass -> aClass.getAnnotation(RequestMapping.class).path()[0]);
    }

    public abstract static class UpdateCommand<T> {
        public abstract void apply(T entity);
    }

    public abstract static class CreateCommand<T> {
        public abstract URI getId();

        public abstract void apply(T entity);
    }
}
