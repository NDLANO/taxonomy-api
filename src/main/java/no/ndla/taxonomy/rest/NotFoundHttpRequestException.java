package no.ndla.taxonomy.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundHttpRequestException extends RuntimeException {
    public NotFoundHttpRequestException(String message) {
        super(message);
    }

    public NotFoundHttpRequestException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
