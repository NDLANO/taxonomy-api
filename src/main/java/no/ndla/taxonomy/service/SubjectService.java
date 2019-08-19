package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;

import java.net.URI;

public interface SubjectService {
    void delete(URI publicId) throws NotFoundServiceException;
}
