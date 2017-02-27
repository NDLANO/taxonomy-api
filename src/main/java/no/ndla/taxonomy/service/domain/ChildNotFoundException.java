package no.ndla.taxonomy.service.domain;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class ChildNotFoundException extends RuntimeException {
    public ChildNotFoundException(String message) {
        super(message);
    }
}
