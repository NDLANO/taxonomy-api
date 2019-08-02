package no.ndla.taxonomy.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
public class Relevance extends DomainObject {

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RelevanceTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceFilter> resources = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicFilter> topics = new HashSet<>();

    public Relevance() {
        setPublicId(URI.create("urn:relevance:" + UUID.randomUUID()));
    }

    public RelevanceTranslation addTranslation(String languageCode) {
        RelevanceTranslation relevanceTranslation = getTranslation(languageCode).orElse(null);
        if (relevanceTranslation != null) return relevanceTranslation;

        relevanceTranslation = new RelevanceTranslation(this, languageCode);
        translations.add(relevanceTranslation);
        return relevanceTranslation;
    }

    public Set<RelevanceTranslation> getTranslations() {
        return translations;
    }

    public Optional<RelevanceTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(relevanceTranslation -> relevanceTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public void addTranslation(RelevanceTranslation relevanceTranslation) {
        this.translations.add(relevanceTranslation);
        if (relevanceTranslation.getRelevance() != this) {
            relevanceTranslation.setRelevance(this);
        }
    }

    public void removeTranslation(RelevanceTranslation translation) {
        if (translation.getRelevance() == this) {
            translations.remove(translation);
            if (translation.getRelevance() == this) {
                translation.setRelevance(null);
            }
        }
    }

    public void removeTranslation(String language) {
        getTranslation(language).ifPresent(this::removeTranslation);
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.topics;
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topics.remove(topicFilter);
        if (topicFilter.getRelevance().orElse(null) == this) {
            topicFilter.setRelevance(null);
        }
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        this.topics.add(topicFilter);

        if (topicFilter.getRelevance().orElse(null) != this) {
            topicFilter.setRelevance(this);
        }
    }

    public Set<ResourceFilter> getResourceFilters() {
        return resources;
    }

    public void addResourceFilter(ResourceFilter resourceFilter) {
        this.resources.add(resourceFilter);

        if (resourceFilter.getRelevance().isEmpty() || resourceFilter.getRelevance().get() != this) {
            resourceFilter.setRelevance(this);
        }
    }

    public void removeResourceFilter(ResourceFilter resourceFilter) {
        this.resources.remove(resourceFilter);

        if (resourceFilter.getRelevance().isPresent() && resourceFilter.getRelevance().get() == this) {
            resourceFilter.setRelevance(null);
        }
    }

}
