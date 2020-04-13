package no.ndla.taxonomy.rest;

public class NotFoundHttpResponseException extends RuntimeException {
    public NotFoundHttpResponseException(String message) {
        super(message);
    }
}
