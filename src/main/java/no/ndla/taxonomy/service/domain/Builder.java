package no.ndla.taxonomy.service.domain;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Builder {
    private EntityManager entityManager;

    private Map<String, ResourceTypeBuilder> resourceTypes = new HashMap<>();
    private Map<String, SubjectBuilder> subjects = new HashMap<>();
    private Map<String, TopicBuilder> topics = new HashMap<>();
    private Map<String, ResourceBuilder> resources = new HashMap<>();
    private Map<String, FilterBuilder> filters = new HashMap<>();
    private Map<String, RelevanceBuilder> relevances = new HashMap<>();

    public Builder(EntityManager entityManager) {
        this.entityManager = entityManager;
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

    public Filter filter(String key) {
        return filter(key, null);
    }

    public Filter filter(Consumer<FilterBuilder> consumer) {
        return filter(null, consumer);
    }

    public Filter filter() {
        return filter(null, null);
    }

    public Filter filter(String key, Consumer<FilterBuilder> consumer) {
        FilterBuilder filter = getFilterBuilder(key);
        if (null != consumer) consumer.accept(filter);
        return filter.filter;
    }

    private FilterBuilder getFilterBuilder(String key) {
        if (key == null) return new FilterBuilder();
        if (!filters.containsKey(key)) filters.put(key, new FilterBuilder());
        return filters.get(key);
    }

    public Relevance relevance(String key) {
        return relevance(key, null);
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
        if (key == null) return new RelevanceBuilder();
        if (!relevances.containsKey(key)) relevances.put(key, new RelevanceBuilder());
        return relevances.get(key);
    }

    private SubjectBuilder getSubjectBuilder(String key) {
        if (key == null) return new SubjectBuilder();
        if (!subjects.containsKey(key)) subjects.put(key, new SubjectBuilder());
        return subjects.get(key);
    }

    private TopicBuilder getTopicBuilder(String key) {
        if (key == null) return new TopicBuilder();
        if (!topics.containsKey(key)) topics.put(key, new TopicBuilder());
        return topics.get(key);
    }

    private ResourceTypeBuilder getResourceTypeBuilder(String key) {
        if (key == null) return new ResourceTypeBuilder();
        if (!resourceTypes.containsKey(key)) resourceTypes.put(key, new ResourceTypeBuilder());
        return resourceTypes.get(key);
    }

    private ResourceBuilder getResourceBuilder(String key) {
        if (key == null) return new ResourceBuilder();
        if (!resources.containsKey(key)) resources.put(key, new ResourceBuilder());
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
        return subject.subject;
    }

    public Subject subject(Consumer<SubjectBuilder> consumer) {
        return subject(null, consumer);
    }

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
            return this;
        }


        public SubjectBuilder topic(Consumer<TopicBuilder> consumer) {
            return topic(null, consumer);
        }

        public SubjectBuilder topic(Topic topic) {
            SubjectTopic subjectTopic = subject.addTopic(topic);
            entityManager.persist(subjectTopic);
            return this;
        }

        public SubjectBuilder filter(String key) {
            return filter(key, null);
        }

        public SubjectBuilder filter(String key, Consumer<FilterBuilder> consumer) {
            FilterBuilder filterBuilder = getFilterBuilder(key);
            if (null != consumer) consumer.accept(filterBuilder);
            filter(filterBuilder.filter);
            return this;
        }

        public SubjectBuilder filter(Consumer<FilterBuilder> consumer) {
            return filter(null, consumer);
        }

        public SubjectBuilder filter(Filter filter) {
            subject.addFilter(filter);
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
            return this;
        }
    }

    public class SubjectTranslationBuilder {
        private SubjectTranslation subjectTranslation;

        public SubjectTranslationBuilder(SubjectTranslation subjectTranslation) {
            this.subjectTranslation = subjectTranslation;
        }

        public SubjectTranslationBuilder name(String name) {
            subjectTranslation.setName(name);
            return this;
        }
    }

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

        public TopicBuilder subtopic() {
            return subtopic(null, null);
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
            entityManager.persist(topic.addSubtopic(subtopic));
            return this;
        }

        public TopicBuilder resource() {
            return resource(null, null);
        }

        public TopicBuilder resource(String resourceKey) {
            return resource(resourceKey, null);
        }

        public TopicBuilder resource(Consumer<ResourceBuilder> consumer) {
            return resource(null, consumer);
        }

        public TopicBuilder resource(String resourceKey, Consumer<ResourceBuilder> consumer) {
            ResourceBuilder resource = getResourceBuilder(resourceKey);
            if (null != consumer) consumer.accept(resource);
            return resource(resource.resource);
        }

        public TopicBuilder resource(Resource resource) {
            TopicResource topicResource = topic.addResource(resource);
            entityManager.persist(topicResource);
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
            return this;
        }

        public TopicBuilder filter(Filter filter, Relevance relevance) {
            TopicFilter topicFilter = topic.addFilter(filter, relevance);
            entityManager.persist(topicFilter);
            return this;
        }
    }

    public class TopicTranslationBuilder {
        private TopicTranslation topicTranslation;

        public TopicTranslationBuilder(TopicTranslation topicTranslation) {
            this.topicTranslation = topicTranslation;
        }

        public TopicTranslationBuilder name(String name) {
            topicTranslation.setName(name);
            return this;
        }
    }

    public class ResourceBuilder {
        private final Resource resource;

        public ResourceBuilder() {
            resource = new Resource();
            entityManager.persist(resource);
        }

        public ResourceBuilder name(String name) {
            resource.name(name);
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
            return this;
        }

        public ResourceBuilder filter(Filter filter, Relevance relevance) {
            ResourceFilter resourceFilter = resource.addFilter(filter, relevance);
            entityManager.persist(resourceFilter);
            return this;
        }
    }

    public class FilterBuilder {
        private final Filter filter;

        public FilterBuilder() {
            filter = new Filter();
            entityManager.persist(filter);
        }

        public FilterBuilder name(String name) {
            filter.setName(name);
            return this;
        }

        public FilterBuilder publicId(String id) {
            filter.setPublicId(URI.create(id));
            return this;
        }

        public FilterBuilder subject(Subject subject) {
            filter.setSubject(subject);
            return this;
        }

        public FilterBuilder translation(String languageCode, Consumer<FilterTranslationBuilder> consumer) {
            FilterTranslation filterTranslation = filter.addTranslation(languageCode);
            entityManager.persist(filterTranslation);
            FilterTranslationBuilder builder = new FilterTranslationBuilder(filterTranslation);
            consumer.accept(builder);
            return this;
        }
    }

    public class FilterTranslationBuilder {
        private FilterTranslation filterTranslation;

        public FilterTranslationBuilder(FilterTranslation filterTranslation) {
            this.filterTranslation = filterTranslation;
        }

        public FilterTranslationBuilder name(String name) {
            filterTranslation.setName(name);
            return this;
        }
    }

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

        public RelevanceBuilder translation(String languageCode, Consumer<RelevanceTranslationBuilder> consumer) {
            RelevanceTranslation relevanceTranslation = relevance.addTranslation(languageCode);
            entityManager.persist(relevanceTranslation);
            RelevanceTranslationBuilder builder = new RelevanceTranslationBuilder(relevanceTranslation);
            consumer.accept(builder);
            return this;
        }
    }

    public class RelevanceTranslationBuilder {
        private RelevanceTranslation relevanceTranslation;

        public RelevanceTranslationBuilder(RelevanceTranslation relevanceTranslation) {
            this.relevanceTranslation = relevanceTranslation;
        }

        public RelevanceTranslationBuilder name(String name) {
            relevanceTranslation.setName(name);
            return this;
        }
    }

    public class ResourceTranslationBuilder {
        private ResourceTranslation resourceTranslation;

        public ResourceTranslationBuilder(ResourceTranslation resourceTranslation) {
            this.resourceTranslation = resourceTranslation;
        }

        public ResourceTranslationBuilder name(String name) {
            resourceTranslation.setName(name);
            return this;
        }
    }

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

        public ResourceTypeBuilder subtype() {
            return subtype(null, null);
        }

        public ResourceTypeBuilder subtype(String resourceTypeKey) {
            return subtype(resourceTypeKey, null);
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

    public class ResourceTypeTranslationBuilder {
        private ResourceTypeTranslation resourceTypeTranslation;

        public ResourceTypeTranslationBuilder(ResourceTypeTranslation resourceTypeTranslation) {
            this.resourceTypeTranslation = resourceTypeTranslation;
        }

        public ResourceTypeTranslationBuilder name(String name) {
            resourceTypeTranslation.setName(name);
            return this;
        }
    }
}