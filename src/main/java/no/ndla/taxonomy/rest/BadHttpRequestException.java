package no.ndla.taxonomy.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadHttpRequestException extends RuntimeException {
    public BadHttpRequestException(String message) {
        super(message);
    }

    public BadHttpRequestException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}