package no.ndla.taxonomy.service.exceptions;

public class InvalidArgumentServiceException extends RuntimeException {
    public InvalidArgumentServiceException() {
    }

    public InvalidArgumentServiceException(String message) {
        super(message);
    }

    public InvalidArgumentServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArgumentServiceException(Throwable cause) {
        super(cause);
    }
}
