/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest;

import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
    private String createErrorBody(RuntimeException exception) {
        try {
            final var jsonObject = new JSONObject();
            jsonObject.put("error", exception.getMessage());
            return jsonObject.toString();
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    private HttpHeaders createHeaders() {
        final var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    protected ResponseEntity<String> handleServiceUnavailableException(RuntimeException exception) {
        return new ResponseEntity<>(
                createErrorBody(exception), createHeaders(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({NotFoundServiceException.class, NotFoundHttpResponseException.class})
    protected ResponseEntity<String> handleNotFoundServiceException(RuntimeException exception) {
        return new ResponseEntity<>(
                createErrorBody(exception), createHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({DuplicateConnectionException.class})
    protected ResponseEntity<String> handleConflictExceptions(RuntimeException exception) {
        return new ResponseEntity<>(
                createErrorBody(exception), createHeaders(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidArgumentServiceException.class, IllegalArgumentException.class})
    protected ResponseEntity<String> handleInvalidArgumentExceptions(RuntimeException exception) {
        return new ResponseEntity<>(
                createErrorBody(exception), createHeaders(), HttpStatus.BAD_REQUEST);
    }
}
