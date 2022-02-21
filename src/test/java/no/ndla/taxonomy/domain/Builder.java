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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class Builder {
    private final EntityManager entityManager;
    private final CachedUrlUpdaterService cachedUrlUpdaterService;
    private final Map<String, VersionBuilder> versions = new HashMap<>();
    private final Map<String, ResourceTypeBuilder> resourceTypes = new HashMap<>();
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

    public Version version() {
        return version(null, null);
    }

    public Version version(String key) {
        return version(key, null);
    }

    public Version version(Consumer<VersionBuilder> consumer) {
        return version(null, consumer);
    }

    public Version version(String key, Consumer<VersionBuilder> consumer) {
        VersionBuilder builder = getVersionBuilder(key);
        if (null != consumer)
            consumer.accept(builder);

        entityManager.persist(builder.version);

        return builder.version;
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
        if (null != consumer)
            consumer.accept(resource);
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
        if (null != consumer)
            consumer.accept(resourceType);
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
        if (null != consumer)
            consumer.accept(relevance);
        return relevance.relevance;
    }

    private VersionBuilder getVersionBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        versions.putIfAbsent(key, new VersionBuilder());
        return versions.get(key);
    }

    private RelevanceBuilder getRelevanceBuilder(String key) {
        if (key == null) {
            key = createKey();
        }
        relevances.putIfAbsent(key, new RelevanceBuilder());
        return relevances.get(key);
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
        if (null != consumer)
            consumer.accept(node);

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
    public static class VersionBuilder {
        private Version version;

        public VersionBuilder() {
            this.version = new Version();

        }

        public VersionBuilder publicId(URI publicId) {
            version.setPublicId(publicId);
            return this;
        }

        public VersionBuilder type(VersionType versionType) {
            version.setVersionType(versionType);
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
            if (null != consumer)
                consumer.accept(resourceTypeBuilder);
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
            if (null != consumer)
                consumer.accept(resourceTypeBuilder);
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

        public ResourceBuilder isVisible(boolean visible) {
            resource.getMetadata().setVisible(visible);
            return this;
        }

        public ResourceBuilder grepCode(String code) {
            GrepCode grepCode = new GrepCode();
            grepCode.setCode(code);
            grepCode.addMetadata(resource.getMetadata());
            entityManager.persist(grepCode);

            resource.getMetadata().addGrepCode(grepCode);
            return this;
        }

        public ResourceBuilder customField(String key, String value) {
            CustomField customField = new CustomField();
            customField.setKey(key);
            entityManager.persist(customField);
            CustomFieldValue customFieldValue = new CustomFieldValue();
            customFieldValue.setCustomField(customField);
            customFieldValue.setValue(value);
            customFieldValue.setMetadata(resource.getMetadata());
            entityManager.persist(customFieldValue);

            resource.getMetadata().addCustomFieldValue(customFieldValue);
            return this;
        }

    }

    public UrlMapping urlMapping(Consumer<UrlMappingBuilder> consumer) {
        return urlMapping(null, consumer);
    }

    public UrlMapping urlMapping(String key, Consumer<UrlMappingBuilder> consumer) {
        UrlMappingBuilder urlMapping = getUrlMappingBuilder(key);
        if (null != consumer)
            consumer.accept(urlMapping);
        return urlMapping.urlMapping;
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

        public NodeBuilder isRoot(boolean root) {
            node.setRoot(root);

            cachedUrlUpdaterService.updateCachedUrls(node);
            return this;
        }

        public NodeBuilder isVisible(boolean visible) {
            node.getMetadata().setVisible(visible);
            return this;
        }

        public NodeBuilder grepCode(String code) {
            GrepCode grepCode = new GrepCode();
            grepCode.setCode(code);
            grepCode.addMetadata(node.getMetadata());
            entityManager.persist(grepCode);

            node.getMetadata().addGrepCode(grepCode);
            return this;
        }

        public NodeBuilder customField(String key, String value) {
            CustomField customField = new CustomField();
            List resultList = entityManager.createQuery("select cf from CustomField cf where cf.key = ?1")
                    .setParameter(1, key).getResultList();
            if (resultList.isEmpty()) {
                customField.setKey(key);
                entityManager.persist(customField);
            } else {
                customField = (CustomField) resultList.get(0);
            }
            CustomFieldValue customFieldValue = new CustomFieldValue();
            customFieldValue.setCustomField(customField);
            customFieldValue.setValue(value);
            customFieldValue.setMetadata(node.getMetadata());
            entityManager.persist(customFieldValue);

            node.getMetadata().addCustomFieldValue(customFieldValue);
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
            if (null != consumer)
                consumer.accept(nodeBuilder);
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
            if (null != consumer)
                consumer.accept(resource);

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
