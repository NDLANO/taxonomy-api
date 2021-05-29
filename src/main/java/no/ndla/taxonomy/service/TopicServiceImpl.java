package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TopicServiceImpl implements TopicService {
    private final ResourceRepository resourceRepository;
    private final RelevanceRepository relevanceRepository;
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final EntityConnectionService connectionService;
    private final MetadataApiService metadataApiService;
    private final TopicTreeSorter topicTreeSorter;

    public TopicServiceImpl(ResourceRepository resourceRepository, RelevanceRepository relevanceRepository, TopicRepository topicRepository, TopicSubtopicRepository topicSubtopicRepository,
                            TopicResourceRepository topicResourceRepository, EntityConnectionService connectionService,
                            MetadataApiService metadataApiService, TopicTreeSorter topicTreeSorter) {
        this.resourceRepository = resourceRepository;
        this.relevanceRepository = relevanceRepository;
        this.topicRepository = topicRepository;
        this.topicResourceRepository = topicResourceRepository;
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

    private URI getNewIdFor(URI orig) {
        /*
         * Lazy developer method:
         * - Take the first two parts of the urn (usuallu urn:topic)
         * and append a random UUID.
         *
         * The orElse inbetween below will probably never hit because
         * orig is guaranteed to be valid URI, and hence will contain
         * a minimum of content.
         */
        return URI.create(
                Arrays.stream(orig.toString().split(":"))
                .limit(2).reduce((a, b) -> a+":"+b)
                .orElse("urn:topic") // (*) see above
                + ":" + UUID.randomUUID().toString()
        );
    }

    private Topic copyTopicEntity(Topic topic) {
        final var duplicate = new Topic();
        duplicate.setPublicId(getNewIdFor(topic.getPublicId()));
        duplicate.setContentUri(topic.getContentUri());
        duplicate.setContext(topic.isContext());
        duplicate.setName(topic.getName());
        topic.getTranslations().forEach(orig -> {
            duplicate.addTranslation(orig.getLanguageCode()).setName(orig.getName());
        });
        topic.getTopicResourceTypes().forEach(topicResourceType -> {
            topicResourceType.getResourceType().ifPresent(duplicate::addResourceType);
        });
        return topicRepository.save(duplicate);
    }

    @Override
    @Transactional
    public TopicDTO prepareRecursiveCopy(URI publicId) {
        final var origMetadata = metadataApiService.getMetadataByPublicId(publicId);
        final var origRoot = topicRepository.findByPublicId(publicId);
        final var root = copyTopicEntity(origRoot);
        {
            final var metadata = MetadataDto.of(origMetadata);
            if (metadata.getCustomFields() == null) {
                metadata.setCustomFields(new HashMap<>());
            }
            {
                final var customFields = metadata.getCustomFields();
                customFields.put("copy-of", publicId.toString());
                customFields.put("copy-root", root.getPublicId().toString());
                customFields.put("status", "incomplete");
            }
            metadataApiService.updateMetadataByPublicId(root.getPublicId(), metadata);
        }
        return new TopicDTO(root, null);
    }

    private List<Runnable> doRecursiveCopy(URI rootOrigPublicId, URI rootCopyPublicId, URI originalPublicId, URI copyPublicId) {
        Topic origRoot = topicRepository.findByPublicId(originalPublicId);
        List<Runnable> bucket = new ArrayList<>();
        {
            List<Supplier<List<Runnable>>> subtopicsBucket = new ArrayList<>();
            origRoot.getChildrenTopicSubtopics().forEach(topicSubtopic -> {
                if (topicSubtopic.getTopic().isPresent()) {
                    final URI origSubPublicId;
                    final URI copySubPublicId;

                    {
                        Topic orig = topicSubtopic.getSubtopic().get();
                        origSubPublicId = orig.getPublicId();

                        final var origMetadata = metadataApiService.getMetadataByPublicId(origSubPublicId);

                        Topic copy = copyTopicEntity(orig);
                        copySubPublicId = copy.getPublicId();

                        {
                            final var metadata = MetadataDto.of(origMetadata);
                            if (metadata.getCustomFields() == null) {
                                metadata.setCustomFields(new HashMap<>());
                            }
                            {
                                final var customFields = metadata.getCustomFields();
                                customFields.put("copy-of", rootOrigPublicId.toString());
                                customFields.put("copy-root", rootCopyPublicId.toString());
                                customFields.put("status", "incomplete");
                            }
                            metadataApiService.updateMetadataByPublicId(copySubPublicId, metadata);
                        }
                    }

                    /* TODO - topic metadata */

                    bucket.add(() -> {
                        Topic root = topicRepository.findByPublicId(copyPublicId);
                        Topic copy = topicRepository.findByPublicId(copySubPublicId);
                        final var subtopicRelation = TopicSubtopic.create(root, copy);
                        topicSubtopic.getRelevance().ifPresent(subtopicRelation::setRelevance);
                        topicSubtopicRepository.save(subtopicRelation);
                    });
                    subtopicsBucket.add(() -> {
                        return doRecursiveCopy(rootOrigPublicId, rootCopyPublicId, origSubPublicId, copySubPublicId);
                    });
                }
            });
            subtopicsBucket.forEach(supp -> {
                bucket.addAll(supp.get());
            });
        }
        origRoot.getTopicResources().forEach(topicResource -> {
            if (topicResource.getResource().isPresent()) {
                final URI resourcePublicId;

                /* topic-filters start */
                final Set<URI> filterPublicIds;
                /* topic-filters end */

                {
                    Resource resource = topicResource.getResource().get();
                    resourcePublicId = resource.getPublicId();
                }
                bucket.add(() -> {
                    Topic topic = topicRepository.findByPublicId(copyPublicId);
                    Resource resource = resourceRepository.findByPublicId(resourcePublicId);
                    topicResourceRepository.save(TopicResource.create(topic, resource, topicResource.isPrimary().orElse(false)));
                });
            }
        });
        bucket.add(() -> {
            final var origMetadata = metadataApiService.getMetadataByPublicId(copyPublicId);
            {
                final var metadata = MetadataDto.of(origMetadata);
                if (metadata.getCustomFields() == null) {
                    metadata.setCustomFields(new HashMap<>());
                }
                {
                    final var customFields = metadata.getCustomFields();
                    customFields.put("status", "complete");
                }
                metadataApiService.updateMetadataByPublicId(copyPublicId, metadata);
            }
        });
        return bucket;
    }

    @Override
    @Transactional
    public void runRecursiveCopy(URI originalPublicId, URI copyPublicId) {
        doRecursiveCopy(originalPublicId, copyPublicId, originalPublicId, copyPublicId).forEach(Runnable::run);
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
