package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface TopicService {
    void delete(URI publicId) throws NotFoundServiceException;

    List<ConnectionIndexDTO> getAllConnections(URI topicPublicId) throws NotFoundServiceException;

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode) throws NotFoundServiceException;

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, Collection<URI> filterPublicIds, String languageCode) throws NotFoundServiceException;
}
