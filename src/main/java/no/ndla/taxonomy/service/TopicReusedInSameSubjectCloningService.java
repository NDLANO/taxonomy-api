package no.ndla.taxonomy.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class TopicReusedInSameSubjectCloningService {
    private SubjectTopicRepository subjectTopicRepository;
    private TopicSubtopicRepository topicSubtopicRepository;
    private TopicRepository topicRepository;

    public TopicReusedInSameSubjectCloningService(SubjectTopicRepository subjectTopicRepository, TopicSubtopicRepository topicSubtopicRepository, TopicRepository topicRepository) {
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicRepository = topicRepository;
    }

    public List<SubjectTopic> findSubjectTopics(Topic topic) {
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
                .map(t -> t.getSubjectTopics())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public TopicCloningContext.TopicHierarchyFixReport copyConflictingTopic(URI contentUri) throws TopicIsNotInConflictException {
        return new TopicCloningContext().copyConflictingTopic(contentUri);
    }

    public class TopicCloningContext {
        public TopicHierarchyFixReport copyConflictingTopic(URI contentUri) throws TopicIsNotInConflictException {
            Topic topic = topicRepository.findByPublicId(contentUri);
            Map<URI, Topic> topicObjects = new HashMap<>();
            topicObjects.put(topic.getPublicId(), topic);
            List<Topic> parentLinks = StreamSupport.stream(Spliterators.spliteratorUnknownSize(topic.getParentTopics(), Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
            parentLinks.stream().forEach(t -> topicObjects.put(t.getPublicId(), t));

            List<URI> parents = parentLinks
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
                                .map(subjectLink -> new AbstractMap.SimpleEntry<>(subjectLink.getSubject().getPublicId(), parentId));
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

        public class TopicCloneFix {
            private TopicCloning topicCloning;

            public TopicCloneFix(Topic parentTopic, Topic topic) {
                topicCloning = new TopicCloning(parentTopic, topic);
                {
                    TopicSubtopic link = parentTopic.getChildrenTopicSubtopics().stream()
                            .filter(l -> l.getSubtopic().getPublicId().equals(topic.getPublicId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Link object not found"));
                    parentTopic.getChildrenTopicSubtopics().remove(link);
                    topicRepository.save(parentTopic); // Cascading&orphan is on
                }
                {
                    /*
                     * Make sure we don't restore the old link by refreshing the Topic objects
                     */
                    parentTopic = topicRepository.findByPublicId(parentTopic.getPublicId());
                    Topic clonedTopic = topicRepository.findByPublicId(topicCloning.getClonedTopic().getPublicId());
                    if (parentTopic == null || clonedTopic == null) {
                        throw new RuntimeException("The parent and/or cloned topic objects are gone (race?)");
                    }
                    TopicSubtopic link = new TopicSubtopic(parentTopic, clonedTopic);
                    topicSubtopicRepository.save(link);
                }
            }

            public TopicCloning getTopicCloning() {
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

            public TopicCloning(Topic parentTopic, Topic topic) {
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

                /* subjects */
                if (topic.getSubjectTopics() != null) {
                    Subject primarySubject = null;
                    for (SubjectTopic subjectTopic : topic.getSubjectTopics()) {
                        SubjectTopic clonedSubjectTopic = new SubjectTopic(subjectTopic.getSubject(), clonedTopic);
                        clonedSubjectTopic.setRank(subjectTopic.getRank());
                        if (subjectTopic.isPrimary()) {
                            primarySubject = subjectTopic.getSubject();
                        }
                        clonedTopic.addSubjectTopic(clonedSubjectTopic);
                    }
                    if (primarySubject != null) {
                        clonedTopic.setPrimarySubject(primarySubject);
                    }
                }

                /* resources */
                if (topic.getTopicResources() != null) {
                    for (TopicResource topicResource : topic.getTopicResources()) {
                        TopicResource clonedTopicResource = new TopicResource(clonedTopic, topicResource.getResource());
                        clonedTopicResource.setPrimary(topicResource.isPrimary());
                        clonedTopicResource.setRank(topicResource.getRank());
                        clonedTopic.addTopicResource(clonedTopicResource);
                    }
                }
                /* filters */
                if (topic.getTopicFilters() != null) {
                    for (TopicFilter topicFilter : topic.getTopicFilters()) {
                        clonedTopic.addTopicFilter(new TopicFilter(clonedTopic, topicFilter.getFilter().orElse(null), topicFilter.getRelevance().orElse(null)));
                    }
                }
                /* translations */
                topic.getTranslations().forEach(translation -> clonedTopic.addTranslation(translation.getLanguageCode()).setName(translation.getName()));
                /* resource types */
                topic.getTopicResourceTypes().forEach(topicResourceType -> clonedTopic.addResourceType(topicResourceType.getResourceType()));

                clonedTopic = topicRepository.save(clonedTopic);

                /* subtopics */
                if (topic.getChildrenTopicSubtopics() != null) {
                    for (TopicSubtopic topicSubtopic : topic.getChildrenTopicSubtopics()) {
                        TopicCloning subtopicCloning = new TopicCloning(topic, topicSubtopic.getSubtopic());
                        clonedSubtopics.add(subtopicCloning);
                        TopicSubtopic clonedSubtopic = clonedTopic.addSubtopic(subtopicCloning.getClonedTopic());
                        clonedSubtopic.setPrimary(topicSubtopic.isPrimary());
                        clonedSubtopic.setRank(topicSubtopic.getRank());
                    }
                }

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

        public class TopicHierarchyFixReport {
            private Map<URI, List<URI>> fullSubjectParentsMap;
            private List<URI> cloningTopicRoots;
            private List<TopicCloning> clonedTopics;

            public TopicHierarchyFixReport(Map<URI, List<URI>> fullSubjectParentsMap, List<URI> cloningTopicRoots, List<TopicCloning> clonedTopics) {
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

        public TopicIsNotInConflictException(Topic topic) {
            super("Topic is not reused in the same subject: "+topic.getPublicId().toString());
            this.topic = topic;
        }

        public Topic getTopic() {
            return topic;
        }
    }
}