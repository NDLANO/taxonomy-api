package no.ndla.taxonomy.service.exceptions;

public class ServiceUnavailableException extends Exception {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceUnavailableException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
