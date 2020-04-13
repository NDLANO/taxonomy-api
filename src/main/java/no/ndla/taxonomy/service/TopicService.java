package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface TopicService {
    void delete(URI publicId);

    List<ConnectionIndexDTO> getAllConnections(URI topicPublicId);

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode);

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, Collection<URI> filterPublicIds, String languageCode);
}
