package no.ndla.taxonomy.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.*;

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

    public Iterator<RelevanceTranslation> getTranslations() {
        return translations.iterator();
    }

    public Optional<RelevanceTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(relevanceTranslation -> relevanceTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public void removeTranslation(String language) {
        getTranslation(language).ifPresent(translations::remove);
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

}
