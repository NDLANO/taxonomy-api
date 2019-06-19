package no.ndla.taxonomy.service.exceptions;

public class NotFoundServiceException extends Exception {
    public NotFoundServiceException() {
    }

    public NotFoundServiceException(String message) {
        super(message);
    }

    public NotFoundServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundServiceException(Throwable cause) {
        super(cause);
    }
}
