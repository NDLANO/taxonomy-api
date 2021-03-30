package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.*;

import java.net.URI;
import java.util.List;

public interface NodeService {
    void delete(URI publicId);

    List<NodeDTO> getNodes(String languageCode, URI contentUriFilter);

    List<ConnectionIndexDTO> getAllConnections(URI nodePublicId);

    List<SubNodeIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode);

    List<SubNodeIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, String languageCode);
}
