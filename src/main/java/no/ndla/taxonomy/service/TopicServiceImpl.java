package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TopicServiceImpl implements TopicService {
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final EntityConnectionService connectionService;
    private final MetadataApiService metadataApiService;
    private final TreeSorter topicTreeSorter;

    public TopicServiceImpl(TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository,
                            EntityConnectionService connectionService,
                            MetadataApiService metadataApiService, TreeSorter topicTreeSorter) {
        this.topicRepository = topicRepository;
        this.connectionService = connectionService;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.metadataApiService = metadataApiService;
        this.topicTreeSorter = topicTreeSorter;
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
    @InjectMetadata
    public List<TopicDTO> getTopics(String languageCode, URI contentUriFilter) {
        final List<Topic> filteredTopics;

        if (contentUriFilter != null) {
            filteredTopics = topicRepository.findAllByContentUriIncludingCachedUrlsAndTranslations(contentUriFilter);
        } else {
            filteredTopics = topicRepository.findAllIncludingCachedUrlsAndTranslations();
        }

        return filteredTopics
                .stream()
                .map(topic -> new TopicDTO(topic, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @MetadataQuery
    public List<TopicDTO> getTopics(String languageCode, URI contentUriFilter, MetadataKeyValueQuery metadataKeyValueQuery) {
        Set<String> publicIds = metadataKeyValueQuery.getDtos().stream()
                .map(MetadataDto::getPublicId)
                .collect(Collectors.toSet());
        return publicIds.stream()
                .map(topicId -> {
                    try {
                        return new URI(topicId);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(topicRepository::findByPublicId)
                .filter(Objects::nonNull)
                .filter(topic -> {
                    /*
                     * I don't think this combination of queries will be normal,
                     * but it's easy to implement something that probably works.
                     */
                    if (contentUriFilter == null) {
                        return true;
                    } else {
                        return contentUriFilter.equals(topic.getContentUri());
                    }
                })
                .map(topic -> new TopicDTO(topic, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @InjectMetadata
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
    @InjectMetadata
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, URI subjectPublicId, String languageCode) {
        // NOTE: Since no filter is specified, it defaults to the same as specifying i list of all filters that exists for the given subject

        return getFilteredSubtopicConnections(
                topicPublicId,
                languageCode);
    }

    @Override
    @InjectMetadata
    public List<SubTopicIndexDTO> getFilteredSubtopicConnections(URI topicPublicId, String languageCode) {
        final List<TopicSubtopic> subtopicConnections;
        subtopicConnections = topicSubtopicRepository.findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(topicPublicId);

        final var wrappedList =
                subtopicConnections.stream()
                        .map(topicSubtopic -> new SubTopicIndexDTO(topicSubtopic, languageCode))
                        .collect(Collectors.toUnmodifiableList());

        return topicTreeSorter.sortList(wrappedList);
    }
}
