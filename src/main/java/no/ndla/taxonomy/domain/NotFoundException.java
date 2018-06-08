package no.ndla.taxonomy.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String type, Object id) {
        super(type + " with id " + id + " not found");
    }

    public NotFoundException(String url) {
        super("Element with url " + url + " not found");
    }

}
