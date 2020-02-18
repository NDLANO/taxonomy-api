package no.ndla.taxonomy.domain.exceptions;


import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class SubjectRequiredException extends RuntimeException {
    public SubjectRequiredException() {
        super("Subject can not be left blank");
    }
}
