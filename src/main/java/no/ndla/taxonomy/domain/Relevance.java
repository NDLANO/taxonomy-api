package no.ndla.taxonomy.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class Relevance extends DomainObject {

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RelevanceTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceFilter> resourceFilters = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TopicFilter> topicFilters = new HashSet<>();

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
        return translations.stream().collect(Collectors.toUnmodifiableSet());
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
        return this.topicFilters.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topicFilters.remove(topicFilter);
        if (topicFilter.getRelevance().orElse(null) == this) {
            topicFilter.disassociate();
        }
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        if (topicFilter.getRelevance().orElse(null) != this) {
            throw new IllegalArgumentException("TopicFilter must have Relevance set before associating with Relevance");
        }

        this.topicFilters.add(topicFilter);
    }

    public Set<ResourceFilter> getResourceFilters() {
        return resourceFilters.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addResourceFilter(ResourceFilter resourceFilter) {
        if (resourceFilter.getRelevance().orElse(null) != this) {
            throw new IllegalArgumentException("ResourceFilter must have Relevance set before associating with Relevance");
        }

        this.resourceFilters.add(resourceFilter);
    }

    public void removeResourceFilter(ResourceFilter resourceFilter) {
        this.resourceFilters.remove(resourceFilter);

        if (resourceFilter.getRelevance().orElse(null) == this) {
            resourceFilter.disassociate();
        }
    }

}
