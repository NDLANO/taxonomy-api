package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class Filter extends DomainObject {

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "filter")
    private Set<FilterTranslation> translations = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceFilter> resources = new HashSet<>();

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public void addTranslation(FilterTranslation filterTranslation) {
        this.translations.add(filterTranslation);
        if (filterTranslation.getFilter() != this) {
            filterTranslation.setFilter(this);
        }
    }

    public void removeTranslation(FilterTranslation translation) {
        if (translation.getFilter() == this) {
            translations.remove(translation);
            if (translation.getFilter() == this) {
                translation.setFilter(null);
            }
        }
    }

    public Set<FilterTranslation> getTranslations() {
        return translations.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Optional<FilterTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(filterTranslation -> filterTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public void removeTranslation(String language) {
        getTranslation(language).ifPresent(this::removeTranslation);
    }

    public void setSubject(Subject subject) {
        final var previousSubject = this.subject;

        this.subject = subject;

        if (previousSubject != null && previousSubject.getFilters().contains(this)) {
            previousSubject.removeFilter(this);
        }

        if (subject != null && !subject.getFilters().contains(this)) {
            subject.addFilter(this);
        }
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(this.subject);
    }

    public void removeResourceFilter(ResourceFilter resourceFilter) {
        this.resources.remove(resourceFilter);

        if (resourceFilter.getFilter() == this) {
            resourceFilter.disassociate();
        }
    }

    public void removeTopicFilter(TopicFilter topicFilter) {
        this.topics.remove(topicFilter);

        if (topicFilter.getFilter().orElse(null) == this) {
            topicFilter.disassociate();
        }
    }

    public Set<ResourceFilter> getResourceFilters() {
        return this.resources.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addResourceFilter(ResourceFilter resourceFilter) {
        if (resourceFilter.getFilter() != this) {
            throw new IllegalArgumentException("Filter must be set on ResourceFilter before associating with ResourceFilter");
        }

        this.resources.add(resourceFilter);
    }

    public Set<TopicFilter> getTopicFilters() {
        return this.topics.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addTopicFilter(TopicFilter topicFilter) {
        if (topicFilter.getFilter().orElse(null) != this) {
            throw new IllegalArgumentException("TopicFilter must have Filter set before associating with Filter");
        }

        this.topics.add(topicFilter);
    }

    @PreRemove
    void preRemove() {
        new HashSet<>(resources).forEach(this::removeResourceFilter);
        new HashSet<>(topics).forEach(this::removeTopicFilter);
        setSubject(null);
    }
}