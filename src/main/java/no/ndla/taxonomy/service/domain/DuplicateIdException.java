package no.ndla.taxonomy.service.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateIdException extends RuntimeException {
    public DuplicateIdException(String id) {
        super("Object with id " + id + " already exists");
    }
}
