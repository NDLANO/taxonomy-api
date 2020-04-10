package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TopicServiceImpl implements TopicService {
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final EntityConnectionService connectionService;
    private final FilterRepository filterRepository;
    private final MetadataApiService metadataApiService;

    public TopicServiceImpl(TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository,
                            FilterRepository filterRepository, EntityConnectionService connectionService,
                            MetadataApiService metadataApiService) {
        this.topicRepository = topicRepository;
        this.connectionService = connectionService;
        this.filterRepository = filterRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.metadataApiService = metadataApiService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) throws NotFoundServiceException, ServiceUnavailableException {
        final var topicToDelete = topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Topic was not found"));

        connectionService.replacePrimaryConnectionsFor(topicToDelete);

        topicRepository.delete(topicToDelete);
        topicRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }

    @Override
    public List<ConnectionIndexDTO> getAllConnections(URI topicPublicId) throws NotFoundServiceException {
        final var topic = topicRepository.findFirstByPublicId(topicPublicId).orElseThrow(() -> new NotFoundServiceException("Topic was not found"));

        return Stream.concat(
                connectionService.getParentConnections(topic)
                        .stream()
                        .map(ConnectionIndexDTO::parentConnection),
                connectionService.getChildConnections(topic)
                        .stream()
                        .filter(entity -> entity instanceof TopicSubtopic)
                        .map(ConnectionIndexDTO::childConnection)
        ).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode) throws NotFoundServiceException {
        return getFilteredSubtopicConnections(
                topicPublicId,
                filterRepository.findAllBySubjectPublicId(subjectPublicId).stream().map(Filter::getPublicId).collect(Collectors.toSet()),
                languageCode);
    }

    @Override
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, Collection<URI> filterPublicIds, String languageCode) throws NotFoundServiceException {
        final Collection<TopicSubtopic> subtopicConnections;
        if (filterPublicIds != null && filterPublicIds.size() > 0) {
            subtopicConnections = topicSubtopicRepository.findAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(topicPublicId, filterPublicIds);
        } else {
            subtopicConnections = topicSubtopicRepository.findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(topicPublicId);
        }

        return subtopicConnections.stream()
                .map(topicSubtopic -> new SubTopicIndexDTO(topicSubtopic, languageCode))
                .collect(Collectors.toUnmodifiableList());
    }
}
