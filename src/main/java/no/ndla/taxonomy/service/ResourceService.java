package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;

import java.net.URI;

public interface ResourceService {
    void delete(URI id) throws NotFoundServiceException, ServiceUnavailableException;
}
