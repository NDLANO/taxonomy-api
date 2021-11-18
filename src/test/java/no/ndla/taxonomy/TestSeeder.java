/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * This class replaces some SQL files that was used to seed the database for various tests. The SQL statements has been
 * rewritten as JPA so any in-application triggers can run.
 *
 * <p>
 * Method names refers to the old SQL file name
 */
@Transactional
@Component
public class TestSeeder {
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private final RelevanceRepository relevanceRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final NodeRepository nodeRepository;
    private final NodeConnectionRepository nodeConnectionRepository;
    private final NodeResourceRepository nodeResourceRepository;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final VersionRepository versionRepository;

    public TestSeeder(SubjectRepository subjectRepository, TopicRepository topicRepository,
            ResourceRepository resourceRepository, RelevanceRepository relevanceRepository,
            ResourceTypeRepository resourceTypeRepository, SubjectTopicRepository subjectTopicRepository,
            TopicSubtopicRepository topicSubtopicRepository, TopicResourceRepository topicResourceRepository,
            NodeRepository nodeRepository, NodeConnectionRepository nodeConnectionRepository,
            NodeResourceRepository nodeResourceRepository, CachedUrlUpdaterService cachedUrlUpdaterService,
            VersionRepository versionRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.relevanceRepository = relevanceRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.nodeRepository = nodeRepository;
        this.nodeConnectionRepository = nodeConnectionRepository;
        this.nodeResourceRepository = nodeResourceRepository;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
        this.versionRepository = versionRepository;
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

    private Resource createResource(String publicId, String name, String contentUri) {
        final var resource = new Resource(versionRepository.findFirstByVersionType(VersionType.PUBLISHED));

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

    private TopicResource createTopicResource(String publicId, Topic topic, Resource resource, Boolean isPrimary,
            Integer rank) {
        return createTopicResource(publicId, topic, resource, isPrimary, rank, null);
    }

    private TopicResource createTopicResource(String publicId, Topic topic, Resource resource, Boolean isPrimary,
            Integer rank, Relevance relevance) {
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

        topicResource.setRelevance(relevance);

        topicResource.getTopic().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return topicResourceRepository.saveAndFlush(topicResource);
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

    private ResourceResourceType createResourceResourceType(String publicId, Resource resource,
            ResourceType resourceType) {
        final var resourceResourceType = ResourceResourceType.create(resource, resourceType);

        if (publicId != null) {
            resourceResourceType.setPublicId(URI.create(publicId));
        }

        return resourceResourceType;
    }

    private Node createNode(NodeType nodeType, String publicId, String name, String contentUri, Boolean context) {
        final var node = new Node(nodeType,
                versionRepository.findFirstByVersionType(VersionType.PUBLISHED).orElseThrow());

        if (publicId != null) {
            node.setPublicId(URI.create(publicId));
        }
        if (name != null) {
            node.setName(name);
        }
        if (contentUri != null) {
            node.setContentUri(URI.create(contentUri));
        }
        if (context != null) {
            node.setContext(context);
        }

        nodeRepository.saveAndFlush(node);

        cachedUrlUpdaterService.updateCachedUrls(node);

        return node;
    }

    private NodeConnection createNodeConnection(String publicId, Node parent, Node child, Integer rank) {
        return createNodeConnection(publicId, parent, child, rank, null);
    }

    private NodeConnection createNodeConnection(String publicId, Node parent, Node child, Integer rank,
            Relevance relevance) {
        final var nodeConnection = NodeConnection.create(parent, child);

        if (publicId != null) {
            nodeConnection.setPublicId(URI.create(publicId));
        }

        if (rank != null) {
            nodeConnection.setRank(rank);
        }

        if (relevance != null) {
            nodeConnection.setRelevance(relevance);
        }

        nodeConnectionRepository.saveAndFlush(nodeConnection);

        nodeConnection.getParent().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return nodeConnection;
    }

    private NodeResource createNodeResource(String publicId, Node node, Resource resource, Boolean isPrimary,
            Integer rank) {
        return createNodeResource(publicId, node, resource, isPrimary, rank, null);
    }

    private NodeResource createNodeResource(String publicId, Node node, Resource resource, Boolean isPrimary,
            Integer rank, Relevance relevance) {
        final var nodeResource = NodeResource.create(node, resource);

        if (publicId != null) {
            nodeResource.setPublicId(URI.create(publicId));
        }

        if (isPrimary != null) {
            nodeResource.setPrimary(isPrimary);
        }

        if (rank != null) {
            nodeResource.setRank(rank);
        }

        nodeResource.setRelevance(relevance);

        nodeResource.getNode().ifPresent(cachedUrlUpdaterService::updateCachedUrls);

        return nodeResourceRepository.saveAndFlush(nodeResource);
    }

    private void clearAll() {
        resourceTypeRepository.deleteAllAndFlush();
        relevanceRepository.deleteAllAndFlush();
        resourceRepository.deleteAllAndFlush();
        topicRepository.deleteAllAndFlush();
        subjectRepository.deleteAllAndFlush();
    }

    public void recursiveTopicsBySubjectIdAndFiltersTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, F=Filter)

        // S:1
        // - ST:1 (F:1)
        // - TST: 1-1 (F:1)
        // - ST:2 (F:2)
        // - TST:2-1 (F:2)
        // - ST:3 (F:1)
        // - TST:3-1 (F:1)
        // - TST:3-2 (F:1)
        // - TST:3-3 (F:2)

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

        final var relevance1 = createRelevance("urn:relevance:core", "Kjernestoff");
    }

    public void recursiveNodesBySubjectNodeIdAndRelevanceTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = Relevance)

        // S:1
        // - ST:1 (R:1)
        // - TST: 1-1 (R:1)
        // - ST:2 (R:2)
        // - TST:2-1 (R:2)
        // - ST:3 (R:1)
        // - TST:3-1 (R:1)
        // - TST:3-2 (R:1)
        // - TST:3-3 (R:2)

        // NOTE ST:3 does not have F:2 but should "inherit" it because one of the subtopics has F:2

        clearAll();

        final var core = createRelevance("urn:relevance:core", "Kjernestoff");
        final var supp = createRelevance("urn:relevance:supplementary", "Tilleggstoff");

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, core);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2, supp);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3, core);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, core);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1, supp);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1, core);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2, core);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3, supp);
    }

    public void recursiveTopicsBySubjectIdTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic)

        // S:1
        // - ST:1
        // - TST: 1-1
        // - ST:2
        // - TST:2-1
        // - ST:3
        // - TST:3-1
        // - TST:3-2
        // - TST:3-3

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

    public void recursiveNodesBySubjectNodeIdTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic)

        // S:1
        // - ST:1
        // - TST: 1-1
        // - ST:2
        // - TST:2-1
        // - ST:3
        // - TST:3-1
        // - TST:3-2
        // - TST:3-3

        clearAll();
        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3);
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

    public void resourceWithResourceTypeTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.NODE, "urn:topic:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1);

        final var resource1 = createResource("urn:resource:1", "R:1", null);
        final var resource2 = createResource("urn:resource:2", "R:2", null);
        final var resource3 = createResource("urn:resource:3", "R:3", null);

        createNodeResource("urn:topic-resource:1", topic1, resource1, true, 1);
        createNodeResource("urn:topic-resource:2", topic1, resource2, true, 2);
        createNodeResource("urn:topic-resource:3", topic1, resource3, true, 3);

        final var resourceType1 = createResourceType(null, "urn:resourcetype:video", "Video");

        createResourceResourceType("urn:resource-resourcetype:1", resource1, resourceType1);
    }

    public void resourceWithRelevancesTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, R = resource, RC = core, RS = supplementary)
        //
        // S:1
        // - ST:1
        // - R:1 RC
        // - R:2 RC
        // - R:3 RC
        // - R:4 RC
        // - R:5 RC
        // - R:6 RC
        // - R:7 RC
        // - R:8 RC
        // - R:9 RC
        // - R:10 RS
        //

        clearAll();

        final var core = createRelevance("urn:relevance:core", "Core");
        final var supp = createRelevance("urn:relevance:supplementary", "Supplementary");

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, core);

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

        createNodeResource("urn:topic-resource:1", topic1, resource1, true, 1, core);
        createNodeResource("urn:topic-resource:2", topic1, resource2, true, 2, core);
        createNodeResource("urn:topic-resource:3", topic1, resource3, true, 3, core);
        createNodeResource("urn:topic-resource:4", topic1, resource4, true, 4, core);
        createNodeResource("urn:topic-resource:5", topic1, resource5, true, 5, core);
        createNodeResource("urn:topic-resource:6", topic1, resource6, true, 6, core);
        createNodeResource("urn:topic-resource:7", topic1, resource7, true, 7, core);
        createNodeResource("urn:topic-resource:8", topic1, resource8, true, 8, core);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 9, core);
        createNodeResource("urn:topic-resource:10", topic1, resource10, true, 10, supp);
    }

    public void resourceWithRelevancesButWithoutFiltersTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2
        //
        // New: Now with 100% less filters!: Split in two subjects based on the filters with
        // a cloned topic structure, but shared resources.

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "Year 1", null, true);
        final var subject2 = createNode(NodeType.SUBJECT, "urn:subject:2", "Year 2", null, true);

        final var f1topic1 = createNode(NodeType.TOPIC, "urn:topic:1:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, f1topic1, 1, null);
        final var f2topic1 = createNode(NodeType.TOPIC, "urn:topic:2:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:2", subject2, f2topic1, 1, null);

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

        final var relevance1 = createRelevance("urn:relevance:core", "Core");
        final var relevance2 = createRelevance("urn:relevance:supplementary", "Supplementary");

        createNodeResource("urn:topic-resource:1", f1topic1, resource1, true, 1, relevance1);
        createNodeResource("urn:topic-resource:2", f1topic1, resource2, true, 2, relevance1);
        createNodeResource("urn:topic-resource:3", f1topic1, resource3, true, 3, relevance1);
        createNodeResource("urn:topic-resource:4", f1topic1, resource4, true, 4, relevance1);
        createNodeResource("urn:topic-resource:5", f1topic1, resource5, true, 5, relevance1);
        createNodeResource("urn:topic-resource:6", f2topic1, resource1, true, 1, relevance2);
        createNodeResource("urn:topic-resource:7", f2topic1, resource2, true, 2, relevance2);
        createNodeResource("urn:topic-resource:8", f2topic1, resource3, true, 3, relevance2);
        createNodeResource("urn:topic-resource:9", f2topic1, resource4, true, 4, relevance2);
        createNodeResource("urn:topic-resource:10", f2topic1, resource5, true, 5, relevance2);
        createNodeResource("urn:topic-resource:11", f2topic1, resource6, true, 6, relevance1);
        createNodeResource("urn:topic-resource:12", f2topic1, resource7, true, 7, relevance1);
        createNodeResource("urn:topic-resource:13", f2topic1, resource8, true, 8, relevance1);
        createNodeResource("urn:topic-resource:14", f2topic1, resource9, true, 9, relevance1);
        createNodeResource("urn:topic-resource:15", f2topic1, resource10, true, 10, relevance1);
    }

    public void resourceWithRelevancesAndOneNullRelevanceButWithoutFiltersTestSetup() {
        // create a test structure with subjects, topics, and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:1
        // - R:2
        //
        // New: Now with 100% less filters!: Split in two subjects based on the filters with
        // a cloned topic structure, but shared resources.

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "Year 1", null, true);
        final var subject2 = createNode(NodeType.SUBJECT, "urn:subject:2", "Year 2", null, true);

        final var f1topic1 = createNode(NodeType.TOPIC, "urn:topic:1:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:1", subject1, f1topic1, 1, null);
        final var f2topic1 = createNode(NodeType.TOPIC, "urn:topic:2:1", "ST:1", null, false);
        createNodeConnection("urn:subject-topic:2", subject2, f2topic1, 1, null);

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

        final var relevance1 = createRelevance("urn:relevance:core", "Core");
        final var relevance2 = createRelevance("urn:relevance:supplementary", "Supplementary");

        createNodeResource("urn:topic-resource:1", f1topic1, resource1, true, 1, relevance1);
        createNodeResource("urn:topic-resource:2", f1topic1, resource2, true, 2, relevance1);
        createNodeResource("urn:topic-resource:3", f1topic1, resource3, true, 3, relevance1);
        createNodeResource("urn:topic-resource:4", f1topic1, resource4, true, 4, relevance1);
        createNodeResource("urn:topic-resource:5", f1topic1, resource5, true, 5, relevance1);
        createNodeResource("urn:topic-resource:6", f2topic1, resource1, true, 1, relevance2);
        createNodeResource("urn:topic-resource:7", f2topic1, resource2, true, 2, relevance2);
        createNodeResource("urn:topic-resource:8", f2topic1, resource3, true, 3, relevance2);
        createNodeResource("urn:topic-resource:9", f2topic1, resource4, true, 4, relevance2);
        createNodeResource("urn:topic-resource:10", f2topic1, resource5, true, 5, relevance2);
        createNodeResource("urn:topic-resource:11", f2topic1, resource6, true, 6, relevance1);
        createNodeResource("urn:topic-resource:12", f2topic1, resource7, true, 7, relevance1);
        createNodeResource("urn:topic-resource:13", f2topic1, resource8, true, 8, relevance1);
        createNodeResource("urn:topic-resource:14", f2topic1, resource9, true, 9, relevance1);
        createNodeResource("urn:topic-resource:15", f2topic1, resource10, true, 10, null);
    }

    public void resourcesBySubjectIdTestSetup() {
        // create a test structure with subjects, topics, subtopics and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:9 (F:1)
        // - TST: 1-1
        // - R:1 (F:1)
        // - ST:2
        // - TST:2-1
        // - R:2 (F:2)
        // - TST: 2-1-1
        // - R:10 (F:2)
        // - ST:3
        // - TST:3-1
        // - R:3 (F:1)
        // - R:5 (F:1)
        // - R:4 (F:2)
        // - TST:3-2
        // - R:6 (F:2)
        // - TST:3-3
        // - R:7 (F:1)
        // - R:8 (F:2)
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);
        final var topic9 = createNode(NodeType.TOPIC, "urn:topic:9", "TST:2-1-1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3);
        createNodeConnection("urn:topic-subtopic:6", topic4, topic9, 1);

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

        createNodeResource("urn:topic-resource:1", topic2, resource1, true, 1);
        createNodeResource("urn:topic-resource:2", topic4, resource2, true, 1);
        createNodeResource("urn:topic-resource:3", topic6, resource3, true, 1);
        createNodeResource("urn:topic-resource:4", topic6, resource4, true, 3);
        createNodeResource("urn:topic-resource:5", topic6, resource5, true, 2);
        createNodeResource("urn:topic-resource:6", topic7, resource6, true, 1);
        createNodeResource("urn:topic-resource:7", topic8, resource7, true, 1);
        createNodeResource("urn:topic-resource:8", topic8, resource8, true, 2);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 1);
        createNodeResource("urn:topic-resource:10", topic9, resource10, true, 1);

        final var relevance1 = createRelevance("urn:relevance:core", "Core");
    }

    public void resourcesBySubjectNodeIdTestSetup() {
        // create a test structure with subjects, topics, subtopics and resources as follows
        // (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)
        //
        // S:1
        // - ST:1
        // - R:9 (F:1)
        // - TST: 1-1
        // - R:1 (F:1)
        // - ST:2
        // - TST:2-1
        // - R:2 (F:2)
        // - TST: 2-1-1
        // - R:10 (F:2)
        // - ST:3
        // - TST:3-1
        // - R:3 (F:1)
        // - R:5 (F:1)
        // - R:4 (F:2)
        // - TST:3-2
        // - R:6 (F:2)
        // - TST:3-3
        // - R:7 (F:1)
        // - R:8 (F:2)
        //

        clearAll();

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "ST:1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "TST:1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "ST:2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "TST:2-1", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "ST:3", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "TST:3-1", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "TST:3-2", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "TST:3-3", null, false);
        final var topic9 = createNode(NodeType.TOPIC, "urn:topic:9", "TST:2-1-1", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1);
        createNodeConnection("urn:subject-topic:2", subject1, topic3, 2);
        createNodeConnection("urn:subject-topic-3", subject1, topic5, 3);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1);
        createNodeConnection("urn:topic-subtopic:2", topic3, topic4, 1);
        createNodeConnection("urn:topic-subtopic:3", topic5, topic6, 1);
        createNodeConnection("urn:topic-subtopic:4", topic5, topic7, 2);
        createNodeConnection("urn:topic-subtopic:5", topic5, topic8, 3);
        createNodeConnection("urn:topic-subtopic:6", topic4, topic9, 1);

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

        createNodeResource("urn:topic-resource:1", topic2, resource1, true, 1);
        createNodeResource("urn:topic-resource:2", topic4, resource2, true, 1);
        createNodeResource("urn:topic-resource:3", topic6, resource3, true, 1);
        createNodeResource("urn:topic-resource:4", topic6, resource4, true, 3);
        createNodeResource("urn:topic-resource:5", topic6, resource5, true, 2);
        createNodeResource("urn:topic-resource:6", topic7, resource6, true, 1);
        createNodeResource("urn:topic-resource:7", topic8, resource7, true, 1);
        createNodeResource("urn:topic-resource:8", topic8, resource8, true, 2);
        createNodeResource("urn:topic-resource:9", topic1, resource9, true, 1);
        createNodeResource("urn:topic-resource:10", topic9, resource10, true, 1);

        final var relevance1 = createRelevance("urn:relevance:core", "Core");
    }

    public void subtopicsByTopicIdAndFiltersTestSetup() {
        // Creates subtopics with different filters
        //
        // Subjects S:1
        // \
        // \
        // Parent topic T1 (has filter F:1 and F:2)
        // |
        // Subtopics T1-1, T1-2, T1-3 (have filter F:1),
        // T1-4, T1-5, T1-6, T1-7 (have filter F:2)
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
        createTopicSubtopic("urn:topic-subtopic:3", topic1, topic4, 3);
        createTopicSubtopic("urn:topic-subtopic:2", topic1, topic3, 2);
        createTopicSubtopic("urn:topic-subtopic:4", topic1, topic5, 4);
        createTopicSubtopic("urn:topic-subtopic:5", topic1, topic6, 5);
        createTopicSubtopic("urn:topic-subtopic:6", topic1, topic7, 6);
        createTopicSubtopic("urn:topic-subtopic:7", topic1, topic8, 7);

        final var relevance1 = createRelevance("urn:relevance:core", "Kjernestoff");
    }

    public void subtopicsByNodeIdAndRelevanceTestSetup() {
        // Creates subtopics with different filters
        //
        // Subjects S:1
        // \
        // \
        // Parent topic T1 (has filter F:1 and F:2)
        // |
        // Subtopics T1-1, T1-2, T1-3 (have filter F:1),
        // T1-4, T1-5, T1-6, T1-7 (have filter F:2)
        //

        clearAll();

        final var core = createRelevance("urn:relevance:core", "Kjernestoff");
        final var supp = createRelevance("urn:relevance:supplementary", "Tilleggstoff");

        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1", "T1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2", "T1-1", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3", "T1-2", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4", "T1-3", null, false);
        final var topic5 = createNode(NodeType.TOPIC, "urn:topic:5", "T1-4", null, false);
        final var topic6 = createNode(NodeType.TOPIC, "urn:topic:6", "T1-5", null, false);
        final var topic7 = createNode(NodeType.TOPIC, "urn:topic:7", "T1-6", null, false);
        final var topic8 = createNode(NodeType.TOPIC, "urn:topic:8", "T1-7", null, false);

        createNodeConnection("urn:subject-topic:1", subject1, topic1, 1, core);

        createNodeConnection("urn:topic-subtopic:1", topic1, topic2, 1, core);
        createNodeConnection("urn:topic-subtopic:3", topic1, topic4, 3, core);
        createNodeConnection("urn:topic-subtopic:2", topic1, topic3, 2, core);
        createNodeConnection("urn:topic-subtopic:4", topic1, topic5, 4, supp);
        createNodeConnection("urn:topic-subtopic:5", topic1, topic6, 5, supp);
        createNodeConnection("urn:topic-subtopic:6", topic1, topic7, 6, supp);
        createNodeConnection("urn:topic-subtopic:7", topic1, topic8, 7, supp);
    }

    public void topicConnectionsTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        //
        // S:1
        // \
        // T:1
        // \
        // T:2
        // / \
        // T:3 T:4
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

    public void topicNodeConnectionsTestSetup() {
        // create a test structure with subjects, topics and subtopics as follows
        //
        // S:1
        // \
        // T:1
        // \
        // T:2
        // / \
        // T:3 T:4
        //

        clearAll();
        final var subject1 = createNode(NodeType.SUBJECT, "urn:subject:1000", "S:1", null, true);

        final var topic1 = createNode(NodeType.TOPIC, "urn:topic:1000", "T1", null, false);
        final var topic2 = createNode(NodeType.TOPIC, "urn:topic:2000", "T2", null, false);
        final var topic3 = createNode(NodeType.TOPIC, "urn:topic:3000", "T3", null, false);
        final var topic4 = createNode(NodeType.TOPIC, "urn:topic:4000", "T4", null, false);

        createNodeConnection("urn:subject-topic:1000", subject1, topic1, 1, null);

        createNodeConnection("urn:topic-subtopic:1000", topic1, topic2, 1, null);
        createNodeConnection("urn:topic-subtopic:2000", topic2, topic3, 1, null);
        createNodeConnection("urn:topic-subtopic:3000", topic2, topic4, 2, null);
    }
}
