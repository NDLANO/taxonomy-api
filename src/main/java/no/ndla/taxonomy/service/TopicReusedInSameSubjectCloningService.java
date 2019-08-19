package no.ndla.taxonomy.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.ndla.taxonomy.domain.SubjectTopic;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicFilter;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class TopicReusedInSameSubjectCloningService {
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final TopicRepository topicRepository;
    private final TopicResourceRepository topicResourceRepository;

    private final EntityConnectionService connectionService;

    public TopicReusedInSameSubjectCloningService(TopicSubtopicRepository topicSubtopicRepository, TopicRepository topicRepository, SubjectTopicRepository subjectTopicRepository,
                                                  TopicResourceRepository topicResourceRepository, EntityConnectionService connectionService) {
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicRepository = topicRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.connectionService = connectionService;
    }

    private List<SubjectTopic> findSubjectTopics(Topic topic) {
        Iterator<Topic> topicIterator = new Iterator<Topic>() {
            private List<Topic> expand = Collections.singletonList(topic);
            private Iterator<Topic> nextTopic = expand.iterator();

            @Override
            public boolean hasNext() {
                return nextTopic != null;
            }

            @Override
            public Topic next() {
                Topic returnTopic = nextTopic.next();
                if (!nextTopic.hasNext()) {
                    expand = expand
                            .stream()
                            .map(Topic::getParentTopics)
                            .map(Collection::iterator)
                            .map(iterator -> Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED))
                            .flatMap(spliterator -> StreamSupport.stream(spliterator, false))
                            .collect(Collectors.toList());
                    nextTopic = expand.iterator();
                    if (!nextTopic.hasNext()) {
                        nextTopic = null;
                    }
                }
                return returnTopic;
            }
        };
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(topicIterator, Spliterator.ORDERED),
                false
        )
                .map(Topic::getSubjectTopics)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public TopicCloningContext.TopicHierarchyFixReport copyConflictingTopic(URI contentUri) throws TopicIsNotInConflictException {
        return new TopicCloningContext().copyConflictingTopic(contentUri);
    }

    public class TopicCloningContext {
        @Transactional
        public TopicHierarchyFixReport copyConflictingTopic(URI contentUri) throws TopicIsNotInConflictException {
            Topic topic = topicRepository.findByPublicId(contentUri);
            Map<URI, Topic> topicObjects = new HashMap<>();
            topicObjects.put(topic.getPublicId(), topic);
            topic.getParentTopics().forEach(t -> topicObjects.put(t.getPublicId(), t));

            List<URI> parents = topic.getParentTopics()
                    .stream()
                    .map(Topic::getPublicId)
                    .collect(Collectors.toList());
            Map<URI, List<URI>> subjectToParentsMap = new HashMap<>();
            parents
                    .stream()
                    .flatMap(parentId -> {
                        Topic parent = topicObjects.get(parentId);
                        List<SubjectTopic> subjectLinks = findSubjectTopics(parent);
                        return subjectLinks
                                .stream()
                                .filter(subjectLink -> subjectLink.getSubject().isPresent())
                                .map(subjectLink -> new AbstractMap.SimpleEntry<>(subjectLink.getSubject().get().getPublicId(), parentId));
                    })
                    .forEach(entry -> {
                        List<URI> parentList = subjectToParentsMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                        URI parentId = entry.getValue();
                        parentList.add(parentId);
                    });
            subjectToParentsMap.values().forEach(Collections::sort); // Be predictive about which ones will be kept and which ones will be cloned

            List<URI> cloningRoots = subjectToParentsMap
                    .values()
                    .stream()
                    .map(Collection::iterator)
                    .filter(Iterator::hasNext)
                    .filter(iterator -> {
                        iterator.next(); // Keep the first
                        return iterator.hasNext(); // If there are more topic roots we'll have to clone the child
                    })
                    .map(iterator -> Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED))
                    .flatMap(spliterator -> StreamSupport.stream(spliterator, false))
                    .collect(Collectors.toList());

            if (cloningRoots.isEmpty()) {
                throw new TopicIsNotInConflictException(topic);
            }

            return new TopicHierarchyFixReport(
                    subjectToParentsMap,
                    cloningRoots,
                    cloningRoots
                            .stream()
                            .map(topicObjects::get)
                            .map(topicRoot -> new TopicCloneFix(topicRoot, topic))
                            .map(TopicCloneFix::getTopicCloning)
                            .collect(Collectors.toList()) // Writes to DB: Actually clones the topics.
            );
        }

        class TopicCloneFix {
            private TopicCloning topicCloning;

            TopicCloneFix(Topic parentTopic, Topic topic) {
                topicCloning = new TopicCloning(parentTopic, topic);
                {
                    connectionService.disconnectTopicSubtopic(parentTopic, topic);
                    try {
                        connectionService.connectTopicSubtopic(parentTopic, topicCloning.getClonedTopic());
                    } catch (DuplicateConnectionException | InvalidArgumentServiceException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            TopicCloning getTopicCloning() {
                return topicCloning;
            }
        }

        private int topicCloningSerial = 0;

        public class TopicCloning {
            @JsonIgnoreProperties({"subjects", "subtopics", "parentTopics", "resources", "filters", "translations", "primaryParentTopic", "topicResourceTypes"})
            private Topic parentTopic;
            @JsonIgnoreProperties({"subjects", "subtopics", "parentTopics", "resources", "filters", "translations", "primaryParentTopic", "topicResourceTypes"})
            private Topic topic;
            @JsonIgnoreProperties({"subjects", "subtopics", "parentTopics", "resources", "filters", "translations", "primaryParentTopic", "topicResourceTypes"})
            private Topic clonedTopic;
            private List<TopicCloning> clonedSubtopics;

            private TopicCloning(Topic parentTopic, Topic topic) {
                this.parentTopic = parentTopic;
                this.clonedSubtopics = new ArrayList<>();
                this.topic = topic;
                URI targetUri;
                try {
                    var splitUrn = topic.getPublicId().toString().split(":");
                    if (splitUrn.length < 2) {
                        throw new RuntimeException("URN format issue: " + topic.getPublicId().toString());
                    }
                    var part1 = StreamSupport.stream(Arrays.spliterator(splitUrn, 0, splitUrn.length - 1), false);
                    var part2 = Stream.of(Integer.toString(++topicCloningSerial));
                    var part3 = Stream.of(splitUrn[splitUrn.length - 1]);
                    targetUri = new URI(
                            Stream.of(part1, part2, part3)
                                    .flatMap(stream -> stream)
                                    .reduce((a, b) -> a + ":" + b)
                                    .orElseThrow(() -> new RuntimeException("Not going to happen!"))
                    );
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                clonedTopic = new Topic();
                clonedTopic.setPublicId(targetUri);
                clonedTopic.setContext(topic.isContext());
                clonedTopic.setContentUri(topic.getContentUri());
                clonedTopic.setName(topic.getName());

                clonedTopic = topicRepository.save(clonedTopic);

                /*
                    DuplicateConnectionExceptions are ignored since they should not happen. If they do, it would
                    not make a difference ignoring it since the resources would already be connected anyway.
                 */

                /* subjects */
                topic.getSubjectTopics()
                        .stream()
                        .filter(subjectTopic -> subjectTopic.getSubject().isPresent())
                        .forEach(subjectTopic -> {
                            try {
                                connectionService.connectSubjectTopic(subjectTopic.getSubject().get(), clonedTopic, subjectTopic.isPrimary(), subjectTopic.getRank());
                            } catch (DuplicateConnectionException | InvalidArgumentServiceException ignored) {
                            }
                        });
                /* resources */
                topic.getTopicResources()
                        .stream()
                        .filter(topicResource -> topicResource.getResource().isPresent())
                        .forEach(topicResource -> {
                            try {
                                connectionService.connectTopicResource(clonedTopic, topicResource.getResource().get(), topicResource.isPrimary(), topicResource.getRank());
                            } catch (DuplicateConnectionException | InvalidArgumentServiceException ignored) {
                    }
                        });
                /* filters */
                topic.getTopicFilters().stream()
                        .filter(topicFilter -> topicFilter.getFilter().isPresent() && topicFilter.getRelevance().isPresent())
                        .forEach(topicFilter -> TopicFilter.create(clonedTopic, topicFilter.getFilter().get(), topicFilter.getRelevance().get()));

                /* translations */
                topic.getTranslations().forEach(translation -> clonedTopic.addTranslation(translation.getLanguageCode()).setName(translation.getName()));
                /* resource types */
                topic.getTopicResourceTypes().stream()
                        .filter(topicResourceType -> topicResourceType.getResourceType().isPresent())
                        .forEach(topicResourceType -> clonedTopic.addResourceType(topicResourceType.getResourceType().get()));

                clonedTopic = topicRepository.save(clonedTopic);

                /* subtopics */
                topic.getChildrenTopicSubtopics().stream()
                        .filter(topicSubtopic -> topicSubtopic.getSubtopic().isPresent())
                        .forEach(topicSubtopic -> {
                            TopicCloning subtopicCloning = new TopicCloning(topic, topicSubtopic.getSubtopic().get());
                            clonedSubtopics.add(subtopicCloning);
                            try {
                                connectionService.connectTopicSubtopic(clonedTopic, subtopicCloning.getClonedTopic(), topicSubtopic.isPrimary(), topicSubtopic.getRank());
                            } catch (DuplicateConnectionException | InvalidArgumentServiceException ignored) {
                            }
                        });

                clonedTopic = topicRepository.save(clonedTopic);
            }

            public Topic getTopic() {
                return topic;
            }

            public Topic getClonedTopic() {
                return clonedTopic;
            }

            public List<TopicCloning> getClonedSubtopics() {
                return clonedSubtopics;
            }

            public Topic getParentTopic() {
                return parentTopic;
            }
        }

        @Transactional
        public class TopicHierarchyFixReport {
            private Map<URI, List<URI>> fullSubjectParentsMap;
            private List<URI> cloningTopicRoots;
            private List<TopicCloning> clonedTopics;

            private TopicHierarchyFixReport(Map<URI, List<URI>> fullSubjectParentsMap, List<URI> cloningTopicRoots, List<TopicCloning> clonedTopics) {
                this.fullSubjectParentsMap = fullSubjectParentsMap;
                this.cloningTopicRoots = cloningTopicRoots;
                this.clonedTopics = clonedTopics;
            }

            public Map<URI, List<URI>> getFullSubjectParentsMap() {
                return fullSubjectParentsMap;
            }

            public List<URI> getCloningTopicRoots() {
                return cloningTopicRoots;
            }

            public List<TopicCloning> getClonedTopics() {
                return clonedTopics;
            }
        }
    }

    public class TopicIsNotInConflictException extends Exception {
        private Topic topic;

        private TopicIsNotInConflictException(Topic topic) {
            super("Topic is not reused in the same subject: "+topic.getPublicId().toString());
            this.topic = topic;
        }

        public Topic getTopic() {
            return topic;
        }
    }
}
