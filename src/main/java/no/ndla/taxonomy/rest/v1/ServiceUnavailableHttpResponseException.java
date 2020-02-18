package no.ndla.taxonomy.rest.v1;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableHttpResponseException extends RuntimeException {
    public ServiceUnavailableHttpResponseException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
