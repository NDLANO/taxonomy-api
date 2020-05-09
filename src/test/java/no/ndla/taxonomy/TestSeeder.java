package no.ndla.taxonomy;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * This class replaces some SQL files that was used to seed the database for various tests. The SQL statements
 * has been rewritten as JPA so any in-application triggers can run.
 * <p>
 * Method names refers to the old SQL file name
 */
@Transactional
@Component
public class TestSeeder {
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private final FilterRepository filterRepository;
    private final RelevanceRepository relevanceRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;

    public TestSeeder(SubjectRepository subjectRepository, TopicRepository topicRepository, ResourceRepository resourceRepository, FilterRepository filterRepository, RelevanceRepository relevanceRepository, ResourceTypeRepository resourceTypeRepository, SubjectTopicRepository subjectTopicRepository, TopicSubtopicRepository topicSubtopicRepository, TopicResourceRepository topicResourceRepository,
                      CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.filterRepository = filterRepository;
        this.relevanceRepository = relevanceRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    private Topic createTopic(String publicId, String name, String contentUri, Boolean context) {
        final var topic = new Topic();
        if (publicId != null) {
            topic.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            topic.setName(name);
        }

        if (contentUri != null) {
            topic.setContentUri(URI.create(contentUri));
        }

        if (context != null) {
            topic.setContext(context);
        }

        topicRepository.saveAndFlush(topic);

        cachedUrlUpdaterService.updateCachedUrls(topic);

        return topic;
    }

    private Subject createSubject(String publicId, String name) {
        final var subject = new Subject();

        if (publicId != null) {
            subject.setPublicId(URI.create(publicId));
        }
        if (name != null) {
            subject.setName(name);
        }

        subjectRepository.saveAndFlush(subject);

        cachedUrlUpdaterService.updateCachedUrls(subject);

        return subject;
    }

    private SubjectTopic createSubjectTopic(String publicId, Topic topic, Subject subject, Integer rank) {
        final var subjectTopic = SubjectTopic.create(subject, topic);

        if (publicId != null) {
            subjectTopic.setPublicId(URI.create(publicId));
        }

        if (rank != null) {
            subjectTopic.setRank(rank);
        }

        subjectTopicRepository.saveAndFlush(subjectTopic);

        subjectTopic.getSubject().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return subjectTopic;
    }

    private TopicSubtopic createTopicSubtopic(String publicId, Topic topic, Topic subTopic, Integer rank) {
        final var topicSubtopic = TopicSubtopic.create(topic, subTopic);

        if (publicId != null) {
            topicSubtopic.setPublicId(URI.create(publicId));
        }

        if (rank != null) {
            topicSubtopic.setRank(rank);
        }

        topicSubtopicRepository.saveAndFlush(topicSubtopic);

        topicSubtopic.getTopic().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return topicSubtopic;
    }

    private Filter createFilter(String publicId, Subject subject, String name) {
        final var filter = new Filter();

        if (publicId != null) {
            filter.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            filter.setName(name);
        }

        if (subject != null) {
            filter.setSubject(subject);
        }

        return filterRepository.save(filter);
    }

    private Relevance createRelevance(String publicId, String name) {
        final var relevance = new Relevance();

        if (publicId != null) {
            relevance.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            relevance.setName(name);
        }

        return relevanceRepository.save(relevance);
    }

    private TopicFilter createTopicFilter(String publicId, Topic topic, Filter filter, Relevance relevance) {
        final var topicFilter = TopicFilter.create(topic, filter, relevance);

        if (publicId != null) {
            topicFilter.setPublicId(URI.create(publicId));
        }

        return topicFilter;
    }

    private Resource createResource(String publicId, String name, String contentUri) {
        final var resource = new Resource();

        if (publicId != null) {
            resource.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            resource.setName(name);
        }

        if (contentUri != null) {
            resource.setContentUri(URI.create(contentUri));
        }

        resourceRepository.saveAndFlush(resource);

        cachedUrlUpdaterService.updateCachedUrls(resource);


        return resource;
    }

    private TopicResource createTopicResource(String publicId, Topic topic, Resource resource, Boolean isPrimary, Integer rank) {
        final var topicResource = TopicResource.create(topic, resource);

        if (publicId != null) {
            topicResource.setPublicId(URI.create(publicId));
        }

        if (isPrimary != null) {
            topicResource.setPrimary(isPrimary);
        }

        if (rank != null) {
            topicResource.setRank(rank);
        }

        topicResource.getTopic().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return topicResourceRepository.saveAndFlush(topicResource);
    }

    private ResourceFilter createResourceFilter(String publicId, Resource resource, Filter filter, Relevance relevance) {
        final var resourceFilter = ResourceFilter.create(resource, filter, relevance);

        if (publicId != null) {
            resourceFilter.setPublicId(URI.create(publicId));
        }

        return resourceFilter;
    }

    private ResourceType createResourceType(ResourceType parent, String publicId, String name) {
        final var resourceType = new ResourceType();

        if (parent != null) {
            resourceType.setParent(parent);
        }

        if (publicId != null) {
            resourceType.setPublicId(URI.create(publicId));
        }

        if (name != null) {
            resourceType.setName(name);
        }

        return resourceTypeRepository.save(resourceType);
    }

    private ResourceResourceType createResourceResourceType(String publicId, Resource resource, ResourceType resourceType) {
        final var resourceResourceType = ResourceResourceType.create(resource, resourceType);

        if (publicId != null) {
            resourceResourceType.setPublicId(URI.create(publicId));
        }

        return resourceResourceType;
    }

    private void clearAll() {
        resourceTypeRepository.deleteAllAndFlush();
        relevanceRepository.deleteAllAndFlush();
        filterRepository.deleteAllAndFlush();
        resourceRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
        subjectRepository.deleteAllAndFlush();
    }

    public void recursiveTopicsBySubjectIdAndFiltersTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, F=Filter)

        // S:1
        //   - ST:1 (F:1)
        //        - TST: 1-1 (F:1)
        //   - ST:2 (F:2)
        //        - TST:2-1 (F:2)
        //   - ST:3 (F:1)
        //        - TST:3-1 (F:1)
        //        - TST:3-2 (F:1)
        //        - TST:3-3 (F:2)

        // NOTE ST:3 does not have F:2 but should "inherit" it because one of the subtopics has F:2

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        final var topic2 = createTopic("urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createTopic("urn:topic:3", "ST:2", null, false);
        final var topic4 = createTopic("urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createTopic("urn:topic:5", "ST:3", null, false);
        final var topic6 = createTopic("urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createTopic("urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createTopic("urn:topic:8", "TST:3-3", null, false);

        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);
        createSubjectTopic("urn:subject-topic:2", topic3, subject1, 2);
        createSubjectTopic("urn:subject-topic-3", topic5, subject1, 3);

        createTopicSubtopic("urn:topic-subtopic:1", topic1, topic2, 1);
        createTopicSubtopic("urn:topic-subtopic:2", topic3, topic4, 1);
        createTopicSubtopic("urn:topic-subtopic:3", topic5, topic6, 1);
        createTopicSubtopic("urn:topic-subtopic:4", topic5, topic7, 2);
        createTopicSubtopic("urn:topic-subtopic:5", topic5, topic8, 3);

        final var filter1 = createFilter("urn:filter:1", subject1, "F:1");
        final var filter2 = createFilter("urn:filter:2", subject1, "F:2");


        final var relevance1 = createRelevance("urn:relevance:core", "Kjernestoff");

        createTopicFilter("urn:topic-filter:1", topic1, filter1, relevance1);
        createTopicFilter("urn:topic-filter:2", topic2, filter1, relevance1);
        createTopicFilter("urn:topic-filter:3", topic3, filter2, relevance1);
        createTopicFilter("urn:topic-filter:4", topic4, filter2, relevance1);
        createTopicFilter("urn:topic-filter:5", topic5, filter1, relevance1);
        createTopicFilter("urn:topic-filter:6", topic6, filter1, relevance1);
        createTopicFilter("urn:topic-filter:7", topic7, filter1, relevance1);
        createTopicFilter("urn:topic-filter:8", topic8, filter2, relevance1);
    }

    public void recursiveTopicsBySubjectIdTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic)

        // S:1
        //   - ST:1
        //        - TST: 1-1
        //   - ST:2
        //        - TST:2-1
        //   - ST:3
        //        - TST:3-1
        //        - TST:3-2
        //        - TST:3-3

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        final var topic2 = createTopic("urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createTopic("urn:topic:3", "ST:2", null, false);
        final var topic4 = createTopic("urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createTopic("urn:topic:5", "ST:3", null, false);
        final var topic6 = createTopic("urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createTopic("urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createTopic("urn:topic:8", "TST:3-3", null, false);

        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);
        createSubjectTopic("urn:subject-topic:2", topic3, subject1, 2);
        createSubjectTopic("urn:subject-topic-3", topic5, subject1, 3);

        createTopicSubtopic("urn:topic-subtopic:1", topic1, topic2, 1);
        createTopicSubtopic("urn:topic-subtopic:2", topic3, topic4, 1);
        createTopicSubtopic("urn:topic-subtopic:3", topic5, topic6, 1);
        createTopicSubtopic("urn:topic-subtopic:4", topic5, topic7, 2);
        createTopicSubtopic("urn:topic-subtopic:5", topic5, topic8, 3);
    }

    public void resourceInDualSubjectsTestSetup() {
        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");
        final var subject2 = createSubject("urn:subject:2", "S:2");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        final var topic2 = createTopic("urn:topic:2", "ST:2", null, false);

        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);
        createSubjectTopic("urn:subject-topic:2", topic2, subject2, 1);

        final var resource1 = createResource("urn:resource:1", "R:1", null);

        createTopicResource("urn:topic-resource:1", topic1, resource1, true, 1);
        createTopicResource("urn:topic-resource:2", topic2, resource1, false, 1);
    }

    public void resourceWithFilterAndTypeTestSetup() {
        //  create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        //   - ST:1
        //      - R:1
        //      - R:2

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);

        createTopicResource("urn:topic-resource:1", topic1, resource1, true, 1);
        createTopicResource("urn:topic-resource:2", topic1, resource2, true, 2);
        createTopicResource("urn:topic-resource:3", topic1, resource3, true, 3);

        final var filter1 = createFilter("urn:filter:1", subject1, "Vg1");
        final var filter2 = createFilter("urn:filter:2", subject1, "Vg2");

        final var relevance1 = createRelevance("urn:relevance:core", "Core");

        createResourceFilter("urn:resource-filter:1", resource1, filter1, relevance1);
        createResourceFilter("urn:resource-filter:2", resource2, filter1, relevance1);
        createResourceFilter("urn:resource-filter:3", resource3, filter2, relevance1);

        final var resourceType1 = createResourceType(null, "urn:resourcetype:video", "Video");

        createResourceResourceType("urn:resource-resourcetype:1", resource1, resourceType1);
    }

    public void resourceWithFiltersAndRelevancesTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        //   - ST:1
        //      - R:1
        //      - R:2
        //

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);


        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createTopicResource("urn:topic-resource:1", topic1, resource1, true, 1);
        createTopicResource("urn:topic-resource:2", topic1, resource2, true, 2);
        createTopicResource("urn:topic-resource:3", topic1, resource3, true, 3);
        createTopicResource("urn:topic-resource:4", topic1, resource4, true, 4);
        createTopicResource("urn:topic-resource:5", topic1, resource5, true, 5);
        createTopicResource("urn:topic-resource:6", topic1, resource6, true, 6);
        createTopicResource("urn:topic-resource:7", topic1, resource7, true, 7);
        createTopicResource("urn:topic-resource:8", topic1, resource8, true, 8);
        createTopicResource("urn:topic-resource:9", topic1, resource9, true, 9);
        createTopicResource("urn:topic-resource:10", topic1, resource10, true, 10);

        final var filter1 = createFilter("urn:filter:1", subject1, "Year 1");
        final var filter2 = createFilter("urn:filter:2", subject1, "Year 2");

        final var relevance1 = createRelevance("urn:relevance:core", "Core");
        final var relevance2 = createRelevance("urn:relevance:supplementary", "Supplementary");

        createResourceFilter("urn:resource-filter:1", resource1, filter1, relevance1); // R 1-5 is core in year 1
        createResourceFilter("urn:resource-filter:2", resource2, filter1, relevance1);
        createResourceFilter("urn:resource-filter:3", resource3, filter1, relevance1);
        createResourceFilter("urn:resource-filter:4", resource4, filter1, relevance1);
        createResourceFilter("urn:resource-filter:5", resource5, filter1, relevance1);

        createResourceFilter("urn:resource-filter:6", resource6, filter2, relevance1); // R 6-10 is core in year 2
        createResourceFilter("urn:resource-filter:7", resource7, filter2, relevance1);
        createResourceFilter("urn:resource-filter:8", resource8, filter2, relevance1);
        createResourceFilter("urn:resource-filter:9", resource9, filter2, relevance1);
        createResourceFilter("urn:resource-filter:10", resource10, filter2, relevance1);

        createResourceFilter("urn:resource-filter:11", resource1, filter2, relevance2); // R 1-5 is supplemental in year 2
        createResourceFilter("urn:resource-filter:12", resource2, filter2, relevance2);
        createResourceFilter("urn:resource-filter:13", resource3, filter2, relevance2);
        createResourceFilter("urn:resource-filter:14", resource4, filter2, relevance2);
        createResourceFilter("urn:resource-filter:15", resource5, filter2, relevance2);
    }

    public void resourcesBySubjectIdTestSetup() {
        // create a test structure with subjects, topics, subtopics and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        //   - ST:1
        //      - R:9 (F:1)
        //        - TST: 1-1
        //            - R:1 (F:1)
        //   - ST:2
        //        - TST:2-1
        //            - R:2 (F:2)
        //            - TST: 2-1-1
        //                  - R:10 (F:2)
        //   - ST:3
        //        - TST:3-1
        //            - R:3 (F:1)
        //            - R:5 (F:1)
        //            - R:4 (F:2)
        //        - TST:3-2
        //            - R:6 (F:2)
        //        - TST:3-3
        //            - R:7 (F:1)
        //            - R:8 (F:2)
        //

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "ST:1", null, false);
        final var topic2 = createTopic("urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createTopic("urn:topic:3", "ST:2", null, false);
        final var topic4 = createTopic("urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createTopic("urn:topic:5", "ST:3", null, false);
        final var topic6 = createTopic("urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createTopic("urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createTopic("urn:topic:8", "TST:3-3", null, false);
        final var topic9 = createTopic("urn:topic:9", "TST:2-1-1", null, false);


        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);
        createSubjectTopic("urn:subject-topic:2", topic3, subject1, 2);
        createSubjectTopic("urn:subject-topic-3", topic5, subject1, 3);

        createTopicSubtopic("urn:topic-subtopic:1", topic1, topic2, 1);
        createTopicSubtopic("urn:topic-subtopic:2", topic3, topic4, 1);
        createTopicSubtopic("urn:topic-subtopic:3", topic5, topic6, 1);
        createTopicSubtopic("urn:topic-subtopic:4", topic5, topic7, 2);
        createTopicSubtopic("urn:topic-subtopic:5", topic5, topic8, 3);
        createTopicSubtopic("urn:topic-subtopic:6", topic4, topic9, 1);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);
        final var resource4 = createResource("urn:resource:4", "R:4", null);
        final var resource5 = createResource("urn:resource:5", "R:5", null);
        final var resource6 = createResource("urn:resource:6", "R:6", null);
        final var resource7 = createResource("urn:resource:7", "R:7", null);
        final var resource8 = createResource("urn:resource:8", "R:8", null);
        final var resource9 = createResource("urn:resource:9", "R:9", null);
        final var resource10 = createResource("urn:resource:10", "R:10", null);

        createTopicResource("urn:topic-resource:1", topic2, resource1, true, 1);
        createTopicResource("urn:topic-resource:2", topic4, resource2, true, 1);
        createTopicResource("urn:topic-resource:3", topic6, resource3, true, 1);
        createTopicResource("urn:topic-resource:4", topic6, resource4, true, 3);
        createTopicResource("urn:topic-resource:5", topic6, resource5, true, 2);
        createTopicResource("urn:topic-resource:6", topic7, resource6, true, 1);
        createTopicResource("urn:topic-resource:7", topic8, resource7, true, 1);
        createTopicResource("urn:topic-resource:8", topic8, resource8, true, 2);
        createTopicResource("urn:topic-resource:9", topic1, resource9, true, 1);
        createTopicResource("urn:topic-resource:10", topic9, resource10, true, 1);


        final var filter1 = createFilter("urn:filter:1", subject1, "F:1");
        final var filter2 = createFilter("urn:filter:2", subject1, "F:2");
        createFilter("urn:filter:3", subject1, "F:3");

        final var relevance1 = createRelevance("urn:relevance:core", "Core");

        createResourceFilter("urn:resource-filter:1", resource1, filter1, relevance1);
        createResourceFilter("urn:resource-filter:2", resource3, filter1, relevance1);
        createResourceFilter("urn:resource-filter:3", resource5, filter1, relevance1);
        createResourceFilter("urn:resource-filter:4", resource7, filter1, relevance1);
        createResourceFilter("urn:resource-filter:5", resource9, filter1, relevance1);
        createResourceFilter("urn:resource-filter:6", resource2, filter2, relevance1);
        createResourceFilter("urn:resource-filter:7", resource4, filter2, relevance1);
        createResourceFilter("urn:resource-filter:8", resource6, filter2, relevance1);
        createResourceFilter("urn:resource-filter:9", resource8, filter2, relevance1);
        createResourceFilter("urn:resource-filter:10", resource10, filter2, relevance1);
    }

    public void subtopicsByTopicIdAndFiltersTestSetup() {
        // Creates subtopics with different filters
        //
        // Subjects       S:1
        //                   \
        //                    \
        // Parent topic       T1 (has filter F:1 and F:2)
        //                     |
        // Subtopics     T1-1, T1-2, T1-3 (have filter F:1),
        //               T1-4, T1-5, T1-6, T1-7 (have filter F:2)
        //

        clearAll();

        final var subject1 = createSubject("urn:subject:1", "S:1");

        final var topic1 = createTopic("urn:topic:1", "T1", null, false);
        final var topic2 = createTopic("urn:topic:2", "T1-1", null, false);
        final var topic3 = createTopic("urn:topic:3", "T1-2", null, false);
        final var topic4 = createTopic("urn:topic:4", "T1-3", null, false);
        final var topic5 = createTopic("urn:topic:5", "T1-4", null, false);
        final var topic6 = createTopic("urn:topic:6", "T1-5", null, false);
        final var topic7 = createTopic("urn:topic:7", "T1-6", null, false);
        final var topic8 = createTopic("urn:topic:8", "T1-7", null, false);

        createSubjectTopic("urn:subject-topic:1", topic1, subject1, 1);

        createTopicSubtopic("urn:topic-subtopic:1", topic1, topic2, 1);
        createTopicSubtopic("urn:topic-subtopic:2", topic1, topic3, 2);
        createTopicSubtopic("urn:topic-subtopic:3", topic1, topic4, 3);
        createTopicSubtopic("urn:topic-subtopic:4", topic1, topic5, 4);
        createTopicSubtopic("urn:topic-subtopic:5", topic1, topic6, 5);
        createTopicSubtopic("urn:topic-subtopic:6", topic1, topic7, 6);
        createTopicSubtopic("urn:topic-subtopic:7", topic1, topic8, 7);

        final var filter1 = createFilter("urn:filter:1", subject1, "F:1");
        final var filter2 = createFilter("urn:filter:2", subject1, "F:2");

        final var relevance1 = createRelevance("urn:relevance:core", "Kjernestoff");

        createTopicFilter("urn:topic-filter:1", topic1, filter1, relevance1);
        createTopicFilter("urn:topic-filter:2", topic1, filter2, relevance1);
        createTopicFilter("urn:topic-filter:3", topic2, filter1, relevance1);
        createTopicFilter("urn:topic-filter:4", topic3, filter1, relevance1);
        createTopicFilter("urn:topic-filter:5", topic4, filter1, relevance1);
        createTopicFilter("urn:topic-filter:6", topic5, filter2, relevance1);
        createTopicFilter("urn:topic-filter:7", topic6, filter2, relevance1);
        createTopicFilter("urn:topic-filter:8", topic7, filter2, relevance1);
        createTopicFilter("urn:topic-filter:9", topic8, filter2, relevance1);
    }

    public void topicConnectionsTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        //
        //         S:1
        //           \
        //            T:1
        //              \
        //                T:2
        //                /  \
        //             T:3   T:4
        //

        clearAll();

        final var subject1 = createSubject("urn:subject:1000", "S:1");

        final var topic1 = createTopic("urn:topic:1000", "T1", null, false);
        final var topic2 = createTopic("urn:topic:2000", "T2", null, false);
        final var topic3 = createTopic("urn:topic:3000", "T3", null, false);
        final var topic4 = createTopic("urn:topic:4000", "T4", null, false);

        createSubjectTopic("urn:subject-topic:1000", topic1, subject1, 1);

        createTopicSubtopic("urn:topic-subtopic:1000", topic1, topic2, 1);
        createTopicSubtopic("urn:topic-subtopic:2000", topic2, topic3, 1);
        createTopicSubtopic("urn:topic-subtopic:3000", topic2, topic4, 2);
    }
}
