package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.*;

@Entity
public class Filter extends DomainObject {

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<FilterTranslation> translations = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ResourceFilter> resources = new HashSet<>();

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicFilter> topics = new HashSet<>();

    public Filter() {
        setPublicId(URI.create("urn:filter:" + UUID.randomUUID()));
    }


    public FilterTranslation addTranslation(String languageCode) {
        final var existingFilterTranslation = getTranslation(languageCode);
        if (existingFilterTranslation.isPresent()) {
            return existingFilterTranslation.get();
        }

        final var filterTranslation = new FilterTranslation(this, languageCode);
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
        this.subject = subject;
    }

    public Subject getSubject() {
        return this.subject;
    }
}