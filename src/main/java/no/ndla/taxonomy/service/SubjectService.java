package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;

import java.net.URI;

public interface SubjectService {
    void delete(URI publicId) throws NotFoundServiceException, ServiceUnavailableException;
}
