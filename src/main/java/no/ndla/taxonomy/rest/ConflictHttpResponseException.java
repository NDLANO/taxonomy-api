package no.ndla.taxonomy.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ConflictHttpResponseException extends RuntimeException {
    public ConflictHttpResponseException(Throwable cause) {
        super(cause);
    }
}
