package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
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
    private final MetadataEntityWrapperService metadataEntityWrapperService;

    public TopicServiceImpl(TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository,
                            FilterRepository filterRepository, EntityConnectionService connectionService,
                            MetadataApiService metadataApiService,
                            MetadataEntityWrapperService metadataEntityWrapperService) {
        this.topicRepository = topicRepository;
        this.connectionService = connectionService;
        this.filterRepository = filterRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.metadataApiService = metadataApiService;
        this.metadataEntityWrapperService = metadataEntityWrapperService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) {
        final var topicToDelete = topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Topic was not found"));

        connectionService.disconnectAllChildren(topicToDelete);

        topicRepository.delete(topicToDelete);
        topicRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }

    @Override
    public List<ConnectionIndexDTO> getAllConnections(URI topicPublicId) {
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
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode, boolean includeMetadata) {
        // NOTE: Since no filter is specified, it defaults to the same as specifying i list of all filters that exists for the given subject

        return getFilteredSubtopicConnections(
                topicPublicId,
                filterRepository.findAllBySubjectPublicId(subjectPublicId).stream().map(Filter::getPublicId).collect(Collectors.toSet()),
                languageCode,
                includeMetadata);
    }

    @Override
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, Collection<URI> filterPublicIds, String languageCode, boolean includeMetadata) {
        final List<TopicSubtopic> subtopicConnections;
        if (filterPublicIds != null && filterPublicIds.size() > 0) {
            subtopicConnections = topicSubtopicRepository.findAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(topicPublicId, filterPublicIds);
        } else {
            subtopicConnections = topicSubtopicRepository.findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(topicPublicId);
        }

        return
                metadataEntityWrapperService.wrapEntities(subtopicConnections, includeMetadata, topicSubtopic -> topicSubtopic.getSubtopic().orElseThrow().getPublicId()).stream()
                        .map(topicSubtopic -> new SubTopicIndexDTO(topicSubtopic, languageCode))
                        .collect(Collectors.toUnmodifiableList());
    }
}
