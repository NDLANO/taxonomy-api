package no.ndla.taxonomy.domain.exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class PrimaryParentRequiredException extends RuntimeException {
    public PrimaryParentRequiredException() {
        super("You cannot unset the primary parent");
    }
}
