package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.*;

@Entity
public class Filter extends DomainObject {

    @OneToMany(mappedBy = "filter", orphanRemoval = true)
    private Set<FilterTranslation> translations = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(orphanRemoval = true, mappedBy = "filter")
    private Set<ResourceFilter> resources = new HashSet<>();

    @OneToMany(orphanRemoval = true, mappedBy = "filter")
    private Set<TopicFilter> topics = new HashSet<>();

    public Filter() {
        setPublicId(URI.create("urn:filter:" + UUID.randomUUID()));
    }


    public FilterTranslation addTranslation(String languageCode) {
        FilterTranslation filterTranslation = getTranslation(languageCode).orElse(null);
        if (filterTranslation != null) return filterTranslation;

        filterTranslation = new FilterTranslation(this, languageCode);
        translations.add(filterTranslation);
        return filterTranslation;
    }

    public Iterator<FilterTranslation> getTranslations() {
        return translations.iterator();
    }

    public Optional<FilterTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(filterTranslation -> filterTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public void removeTranslation(String language) {
        getTranslation(language).ifPresent(translations::remove);
    }

    public void setSubject(Subject subject) {
        if (subject == null && this.subject != null) {
            this.subject.removeFilter(this);
        }

        this.subject = subject;

        if (subject != null && !subject.getFilters().contains(this)) {
            subject.removeFilter(this);
        }
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(this.subject);
    }

    public void removeResourceFilter(ResourceFilter resourceFilter) {
        this.resources.remove(resourceFilter);
        if (resourceFilter.getFilter() == this) {
            resourceFilter.setFilter(null);
        }
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topics.remove(topicFilter);

        if (topicFilter.getFilter().orElse(null) == this) {
            topicFilter.setFilter(null);
        }
    }

    public Set<ResourceFilter> getResourceFilters() {
        return this.resources;
    }

    public void addResourceFilter(ResourceFilter resourceFilter) {
        this.resources.add(resourceFilter);

        if (resourceFilter.getFilter() != this) {
            resourceFilter.setFilter(this);
        }
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.topics;
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        this.topics.add(topicFilter);

        if (topicFilter.getFilter().orElse(null) != this) {
            topicFilter.setFilter(this);
        }
    }

    @PreRemove
    void preRemove() {
        for (var resourceFilter : this.resources.toArray()) {
            removeResourceFilter((ResourceFilter) resourceFilter);
        }
        for (var topicFilter : this.topics.toArray()) {
            removeTopicFilter((TopicFilter) topicFilter);
        }
    }
}