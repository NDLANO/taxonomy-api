/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class Builder {
    private final EntityManager entityManager;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final Map<String, ResourceTypeBuilder> resourceTypes = new HashMap<>();
    private final Map<String, SubjectBuilder> subjects = new HashMap<>();
    private final Map<String, TopicBuilder> topics = new HashMap<>();
    private final Map<String, NodeBuilder> nodes = new HashMap<>();
    private final Map<String, ResourceBuilder> resources = new HashMap<>();
    private final Map<String, RelevanceBuilder> relevances = new HashMap<>();
    private final Map<String, UrlMappingBuilder> cachedUrlOldRigBuilders = new HashMap<>();
    private int keyCounter = 0;

    public Builder(EntityManager entityManager, CachedUrlUpdaterService cachedUrlUpdaterService) {
        this.entityManager = entityManager;
        this.cachedUrlUpdaterService = cachedUrlUpdaterService;
    }

    private String createKey() {
        return "DefaultKey:" + keyCounter++;
    }

    public Topic topic(String key) {
        return topic(key, null);
    }

    public Topic topic(Consumer<TopicBuilder> consumer) {
        return topic(null, consumer);
    }

    public Topic topic() {
        return topic(null, null);
    }

    public Topic topic(String key, Consumer<TopicBuilder> consumer) {
        TopicBuilder topic = getTopicBuilder(key);
        if (null != consumer) consumer.accept(topic);
        return topic.topic;
    }

    public Resource resource() {
        return resource(null, null);
    }

    public Resource resource(String key) {
        return resource(key, null);
    }

    public Resource resource(Consumer<ResourceBuilder> consumer) {
        return resource(null, consumer);
    }

    public Resource resource(String key, Consumer<ResourceBuilder> consumer) {
        ResourceBuilder resource = getResourceBuilder(key);
        if (null != consumer) consumer.accept(resource);
        return resource.resource;
    }

    public ResourceType resourceType(String key) {
        return resourceType(key, null);
    }

    public ResourceType resourceType(Consumer<ResourceTypeBuilder> consumer) {
        return resourceType(null, consumer);
    }

    public ResourceType resourceType(String key, Consumer<ResourceTypeBuilder> consumer) {
        ResourceTypeBuilder resourceType = getResourceTypeBuilder(key);
        if (null != consumer) consumer.accept(resourceType);
        return resourceType.resourceType;
    }

    public Relevance relevance(Consumer<RelevanceBuilder> consumer) {
        return relevance(null, consumer);
    }

    public Relevance relevance() {
        return relevance(null, null);
    }

    public Relevance relevance(String key, Consumer<RelevanceBuilder> consumer) {
        RelevanceBuilder relevance = getRelevanceBuilder(key);
        if (null != consumer) consumer.accept(relevance);
        return relevance.relevance;
    }

    private RelevanceBuilder getRelevanceBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        relevances.putIfAbsent(key, new RelevanceBuilder());
        return relevances.get(key);
    }

    private SubjectBuilder getSubjectBuilder(String key) {
        if (key == null) {
            key = createKey();
        }

        subjects.putIfAbsent(key, new SubjectBuilder());
        return subjects.get(key);
    }

    private TopicBuilder getTopicBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        topics.putIfAbsent(key, new TopicBuilder());
        return topics.get(key);
    }

    private NodeBuilder getNodeBuilder(String key, NodeType nodeType) {
        if (key == null) {
            key = createKey();
        }
        if (nodeType == null) {
            nodeType = NodeType.NODE;
        }
        nodes.putIfAbsent(key, new NodeBuilder(nodeType));
        return nodes.get(key);
    }

    private ResourceTypeBuilder getResourceTypeBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        resourceTypes.putIfAbsent(key, new ResourceTypeBuilder());
        return resourceTypes.get(key);
    }

    private ResourceBuilder getResourceBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        resources.putIfAbsent(key, new ResourceBuilder());
        return resources.get(key);
    }

    public Subject subject(String key) {
        return subject(key, null);
    }

    public Subject subject() {
        return subject(null, null);
    }

    public Subject subject(String key, Consumer<SubjectBuilder> consumer) {
        SubjectBuilder subject = getSubjectBuilder(key);
        if (null != consumer) consumer.accept(subject);

        entityManager.persist(subject.subject);

        cachedUrlUpdaterService.updateCachedUrls(subject.subject);

        return subject.subject;
    }

    public Subject subject(Consumer<SubjectBuilder> consumer) {
        return subject(null, consumer);
    }

    public Node node() {
        return node(null, null, null);
    }

    public Node node(String key) {
        return node(key, null, null);
    }

    public Node node(NodeType nodeType) {
        return node(null, nodeType, null);
    }

    public Node node(Consumer<NodeBuilder> consumer) {
        return node(null, null, consumer);
    }

    public Node node(NodeType nodeType, Consumer<NodeBuilder> consumer) {
        return node(null, nodeType, consumer);
    }

    public Node node(String key, NodeType nodeType, Consumer<NodeBuilder> consumer) {
        NodeBuilder node = getNodeBuilder(key, nodeType);
        if (null != consumer) consumer.accept(node);

        entityManager.persist(node.node);

        cachedUrlUpdaterService.updateCachedUrls(node.node);

        return node.node;
    }


    private UrlMappingBuilder getUrlMappingBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        cachedUrlOldRigBuilders.putIfAbsent(key, new UrlMappingBuilder());
        return cachedUrlOldRigBuilders.get(key);
    }

    @Transactional
    public static class SubjectTranslationBuilder {
        private SubjectTranslation subjectTranslation;

        public SubjectTranslationBuilder(SubjectTranslation subjectTranslation) {
            this.subjectTranslation = subjectTranslation;
        }

        public SubjectTranslationBuilder name(String name) {
            subjectTranslation.setName(name);
            return this;
        }
    }

    @Transactional
    public static class TopicTranslationBuilder {
        private TopicTranslation topicTranslation;

        public TopicTranslationBuilder(TopicTranslation topicTranslation) {
            this.topicTranslation = topicTranslation;
        }

        public TopicTranslationBuilder name(String name) {
            topicTranslation.setName(name);
            return this;
        }
    }

    @Transactional
    public static class NodeTranslationBuilder {
        private NodeTranslation nodeTranslation;

        public NodeTranslationBuilder(NodeTranslation nodeTranslation) {
            this.nodeTranslation = nodeTranslation;
        }

        public NodeTranslationBuilder name(String name) {
            nodeTranslation.setName(name);
            return this;
        }
    }

    @Transactional
    public static class ResourceTranslationBuilder {
        private ResourceTranslation resourceTranslation;

        public ResourceTranslationBuilder(ResourceTranslation resourceTranslation) {
            this.resourceTranslation = resourceTranslation;
        }

        public ResourceTranslationBuilder name(String name) {
            resourceTranslation.setName(name);
            return this;
        }
    }

    @Transactional
    public static class ResourceTypeTranslationBuilder {
        private ResourceTypeTranslation resourceTypeTranslation;

        public ResourceTypeTranslationBuilder(ResourceTypeTranslation resourceTypeTranslation) {
            this.resourceTypeTranslation = resourceTypeTranslation;
        }

        public ResourceTypeTranslationBuilder name(String name) {
            resourceTypeTranslation.setName(name);
            return this;
        }
    }

    @Transactional
    public class RelevanceBuilder {
        private final Relevance relevance;

        public RelevanceBuilder() {
            relevance = new Relevance();
            entityManager.persist(relevance);
        }

        public RelevanceBuilder name(String name) {
            relevance.setName(name);
            return this;
        }

        public RelevanceBuilder publicId(String id) {
            relevance.setPublicId(URI.create(id));
            return this;
        }
    }

    @Transactional
    public static class UrlMappingBuilder {
        private UrlMapping urlMapping;

        public UrlMappingBuilder() {
            urlMapping = new UrlMapping();
        }

        public UrlMappingBuilder oldUrl(String p) {
            urlMapping.setOldUrl(p);
            return this;
        }

        public UrlMappingBuilder public_id(String p) {
            urlMapping.setPublic_id(p);
            return this;
        }

        public UrlMappingBuilder subject_id(String s) {
            urlMapping.setSubject_id(s);
            return this;
        }

    }

    @Transactional
    public class ResourceTypeBuilder {
        private final ResourceType resourceType;

        public ResourceTypeBuilder() {
            resourceType = new ResourceType();
            entityManager.persist(resourceType);
        }

        public ResourceTypeBuilder name(String name) {
            resourceType.name(name);
            return this;
        }

        public ResourceTypeBuilder translation(String languageCode, Consumer<ResourceTypeTranslationBuilder> consumer) {
            ResourceTypeTranslation resourceTypeTranslation = resourceType.addTranslation(languageCode);
            entityManager.persist(resourceTypeTranslation);
            ResourceTypeTranslationBuilder builder = new ResourceTypeTranslationBuilder(resourceTypeTranslation);
            consumer.accept(builder);
            return this;
        }

        public ResourceTypeBuilder subtype(Consumer<ResourceTypeBuilder> consumer) {
            return subtype(null, consumer);
        }

        public ResourceTypeBuilder subtype(String key, Consumer<ResourceTypeBuilder> consumer) {
            ResourceTypeBuilder resourceTypeBuilder = getResourceTypeBuilder(key);
            if (null != consumer) consumer.accept(resourceTypeBuilder);
            subtype(resourceTypeBuilder.resourceType);
            return this;
        }

        public ResourceTypeBuilder subtype(ResourceType subtype) {
            subtype.setParent(resourceType);
            return this;
        }

        public ResourceTypeBuilder publicId(String id) {
            resourceType.setPublicId(URI.create(id));
            return this;
        }
    }

    @Transactional
    public class ResourceBuilder {
        private final Resource resource;

        public ResourceBuilder() {
            resource = new Resource();
            entityManager.persist(resource);
        }

        public ResourceBuilder name(String name) {
            resource.setName(name);
            return this;
        }

        public ResourceBuilder resourceType(ResourceType resourceType) {
            entityManager.persist(resource.addResourceType(resourceType));
            return this;
        }

        public ResourceBuilder resourceType(Consumer<ResourceTypeBuilder> consumer) {
            return resourceType(null, consumer);
        }

        public ResourceBuilder resourceType(String resourceTypeKey, Consumer<ResourceTypeBuilder> consumer) {
            ResourceTypeBuilder resourceTypeBuilder = getResourceTypeBuilder(resourceTypeKey);
            if (null != consumer) consumer.accept(resourceTypeBuilder);
            return resourceType(resourceTypeBuilder.resourceType);
        }

        public ResourceBuilder resourceType(String resourceTypeKey) {
            return resourceType(resourceTypeKey, null);
        }

        public ResourceBuilder contentUri(String contentUri) {
            return contentUri(URI.create(contentUri));
        }

        public ResourceBuilder contentUri(URI contentUri) {
            resource.setContentUri(contentUri);
            return this;
        }

        public ResourceBuilder translation(String languageCode, Consumer<ResourceTranslationBuilder> consumer) {
            ResourceTranslation resourceTranslation = resource.addTranslation(languageCode);
            entityManager.persist(resourceTranslation);
            ResourceTranslationBuilder builder = new ResourceTranslationBuilder(resourceTranslation);
            consumer.accept(builder);
            return this;
        }

        public ResourceBuilder publicId(String id) {
            resource.setPublicId(URI.create(id));

            cachedUrlUpdaterService.updateCachedUrls(resource);

            return this;
        }
    }

    public UrlMapping urlMapping(Consumer<UrlMappingBuilder> consumer) {
        return urlMapping(null, consumer);
    }

    public UrlMapping urlMapping(String key, Consumer<UrlMappingBuilder> consumer) {
        UrlMappingBuilder urlMapping = getUrlMappingBuilder(key);
        if (null != consumer) consumer.accept(urlMapping);
        return urlMapping.urlMapping;
    }

    @Transactional
    public class SubjectBuilder {
        private final Subject subject;

        public SubjectBuilder() {
            subject = new Subject();
            entityManager.persist(subject);
        }

        public SubjectBuilder name(String name) {
            subject.name(name);
            return this;
        }

        public SubjectBuilder topic(String key) {
            return topic(key, null);
        }

        public SubjectBuilder topic(String key, Consumer<TopicBuilder> consumer) {
            TopicBuilder topicBuilder = getTopicBuilder(key);
            if (null != consumer) consumer.accept(topicBuilder);
            topic(topicBuilder.topic);

            cachedUrlUpdaterService.updateCachedUrls(topicBuilder.topic);

            return this;
        }


        public SubjectBuilder topic(Consumer<TopicBuilder> consumer) {
            return topic(null, consumer);
        }

        public SubjectBuilder topic(Topic topic) {
            SubjectTopic subjectTopic = SubjectTopic.create(subject, topic);
            entityManager.persist(subjectTopic);

            cachedUrlUpdaterService.updateCachedUrls(topic);

            return this;
        }

        public SubjectBuilder contentUri(String contentUri) {
            return contentUri(URI.create(contentUri));
        }

        public SubjectBuilder contentUri(URI contentUri) {
            subject.setContentUri(contentUri);
            return this;
        }

        public SubjectBuilder translation(String languageCode, Consumer<SubjectTranslationBuilder> consumer) {
            SubjectTranslation subjectTranslation = subject.addTranslation(languageCode);
            entityManager.persist(subjectTranslation);
            SubjectTranslationBuilder builder = new SubjectTranslationBuilder(subjectTranslation);
            consumer.accept(builder);
            return this;
        }

        public SubjectBuilder publicId(String id) {
            subject.setPublicId(URI.create(id));

            cachedUrlUpdaterService.updateCachedUrls(subject);

            return this;
        }
    }

    @Transactional
    public class TopicBuilder {
        private final Topic topic;

        public TopicBuilder() {
            topic = new Topic();
            entityManager.persist(topic);
        }

        public TopicBuilder name(String name) {
            topic.name(name);
            return this;
        }

        public TopicBuilder subtopic(String topicKey) {
            return subtopic(topicKey, null);
        }

        public TopicBuilder subtopic(Consumer<TopicBuilder> consumer) {
            return subtopic(null, consumer);
        }

        public TopicBuilder subtopic(String key, Consumer<TopicBuilder> consumer) {
            TopicBuilder topicBuilder = getTopicBuilder(key);
            if (null != consumer) consumer.accept(topicBuilder);
            subtopic(topicBuilder.topic);
            return this;
        }

        public TopicBuilder subtopic(Topic subtopic) {
            TopicSubtopic.create(topic, subtopic);

            cachedUrlUpdaterService.updateCachedUrls(subtopic);

            return this;
        }

        public TopicBuilder resource() {
            return resource(null, null);
        }

        public TopicBuilder resource(String resourceKey) {
            return resource(resourceKey, null);
        }

        public TopicBuilder resource(boolean primary, Consumer<ResourceBuilder> consumer) {
            return resource(null, primary, consumer);
        }

        public TopicBuilder resource(Consumer<ResourceBuilder> consumer) {
            return resource(null, consumer);
        }

        public TopicBuilder resource(String resourceKey, Consumer<ResourceBuilder> consumer) {
            return resource(resourceKey, false, consumer);
        }

        public TopicBuilder resource(String resourceKey, boolean primary, Consumer<ResourceBuilder> consumer) {
            ResourceBuilder resource = getResourceBuilder(resourceKey);
            if (null != consumer) consumer.accept(resource);

            return resource(resource.resource, primary);
        }

        public TopicBuilder resource(String resourceKey, boolean primary) {
            return resource(resourceKey, primary, null);
        }

        public TopicBuilder resource(Resource resource) {
            entityManager.persist(TopicResource.create(topic, resource));

            cachedUrlUpdaterService.updateCachedUrls(resource);

            return this;
        }

        public TopicBuilder resource(Resource resource, boolean primary) {
            entityManager.persist(TopicResource.create(topic, resource, primary));

            cachedUrlUpdaterService.updateCachedUrls(resource);

            return this;
        }

        public TopicBuilder contentUri(String contentUri) {
            return contentUri(URI.create(contentUri));
        }

        public TopicBuilder contentUri(URI contentUri) {
            topic.setContentUri(contentUri);
            return this;
        }

        public TopicBuilder translation(String languageCode, Consumer<TopicTranslationBuilder> consumer) {
            TopicTranslation topicTranslation = topic.addTranslation(languageCode);
            entityManager.persist(topicTranslation);
            TopicTranslationBuilder builder = new TopicTranslationBuilder(topicTranslation);
            consumer.accept(builder);

            return this;
        }

        public TopicBuilder publicId(String id) {
            topic.setPublicId(URI.create(id));

            cachedUrlUpdaterService.updateCachedUrls(topic);

            return this;
        }

        public void isContext(boolean b) {
            topic.setContext(b);

            cachedUrlUpdaterService.updateCachedUrls(topic);
        }

    }

    @Transactional
    public class NodeBuilder {
        private final Node node;

        public NodeBuilder(NodeType nodeType) {
            node = new Node(nodeType);
            entityManager.persist(node);
        }

        public NodeBuilder nodeType(NodeType nodeType) {
            node.setNodeType(nodeType);
            cachedUrlUpdaterService.updateCachedUrls(node);
            return this;
        }

        public NodeBuilder name(String name) {
            node.setName(name);
            return this;
        }

        public NodeBuilder contentUri(String contentUri) {
            return contentUri(URI.create(contentUri));
        }

        public NodeBuilder contentUri(URI contentUri) {
            node.setContentUri(contentUri);
            return this;
        }

        public NodeBuilder publicId(String id) {
            node.setPublicId(URI.create(id));

            cachedUrlUpdaterService.updateCachedUrls(node);
            return this;
        }

        public NodeBuilder isContext(boolean b) {
            node.setContext(b);

            cachedUrlUpdaterService.updateCachedUrls(node);
            return this;
        }

        public NodeBuilder child(String nodeKey) {
            return child(nodeKey, null, null);
        }

        public NodeBuilder child(String nodeKey, NodeType nodeType) {
            return child(nodeKey, nodeType, null);
        }

        public NodeBuilder child(Consumer<NodeBuilder> consumer) {
            return child(null, null, consumer);
        }

        public NodeBuilder child(NodeType nodeType, Consumer<NodeBuilder> consumer) {
            return child(null, nodeType, consumer);
        }

        public NodeBuilder child(String key, NodeType nodeType, Consumer<NodeBuilder> consumer) {
            NodeBuilder nodeBuilder = getNodeBuilder(key, nodeType);
            if (null != consumer) consumer.accept(nodeBuilder);
            child(nodeBuilder.node);
            return this;
        }

        public NodeBuilder child(Node child) {
            entityManager.persist(NodeConnection.create(node, child));

            cachedUrlUpdaterService.updateCachedUrls(child);

            return this;
        }

        public NodeBuilder resource() {
            return resource(null, null);
        }

        public NodeBuilder resource(String resourceKey) {
            return resource(resourceKey, null);
        }

        public NodeBuilder resource(boolean primary, Consumer<ResourceBuilder> consumer) {
            return resource(null, primary, consumer);
        }

        public NodeBuilder resource(Consumer<ResourceBuilder> consumer) {
            return resource(null, consumer);
        }

        public NodeBuilder resource(String resourceKey, Consumer<ResourceBuilder> consumer) {
            return resource(resourceKey, false, consumer);
        }

        public NodeBuilder resource(String resourceKey, boolean primary, Consumer<ResourceBuilder> consumer) {
            ResourceBuilder resource = getResourceBuilder(resourceKey);
            if (null != consumer) consumer.accept(resource);

            return resource(resource.resource, primary);
        }

        public NodeBuilder resource(String resourceKey, boolean primary) {
            return resource(resourceKey, primary, null);
        }

        public NodeBuilder resource(Resource resource) {
            entityManager.persist(NodeResource.create(node, resource));

            cachedUrlUpdaterService.updateCachedUrls(resource);

            return this;
        }

        public NodeBuilder resource(Resource resource, boolean primary) {
            entityManager.persist(NodeResource.create(node, resource, primary));

            cachedUrlUpdaterService.updateCachedUrls(resource);

            return this;
        }

        public NodeBuilder translation(String languageCode, Consumer<NodeTranslationBuilder> consumer) {
            NodeTranslation nodeTranslation = node.addTranslation(languageCode);
            entityManager.persist(nodeTranslation);
            NodeTranslationBuilder builder = new NodeTranslationBuilder(nodeTranslation);
            consumer.accept(builder);

            return this;
        }

    }

}