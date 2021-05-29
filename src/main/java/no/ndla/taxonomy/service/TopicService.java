package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface TopicService {
    void delete(URI publicId);

    /*
     * The idea is that a web request to copy a topic will be handled by:
     *  - Calling prepareRecursiveCopy (returns dto)
     *  - Asynchronously calling runRecursiveCopy without waiting for completion
     *  - Return the dto with a 202 Accepted code
     *
     * Key value metadata on the copy will indicate the status of the recursive
     * copy operation.
     */
    TopicDTO prepareRecursiveCopy(URI publicId);
    void runRecursiveCopy(URI originalPublicId, URI copyPublicId);

    List<TopicDTO> getTopics(String languageCode, URI contentUriFilter);

    List<ConnectionIndexDTO> getAllConnections(URI topicPublicId);

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode);

    List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, String languageCode);
}
