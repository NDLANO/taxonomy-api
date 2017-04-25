package no.ndla.taxonomy.service.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Filter extends DomainObject {

    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<FilterTranslation> translations = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public Filter() {
        setPublicId(URI.create("urn:filter:" + UUID.randomUUID()));
    }


    public FilterTranslation addTranslation(String languageCode) {
        FilterTranslation filterTranslation = getTranslation(languageCode);
        if (filterTranslation != null) return filterTranslation;

        filterTranslation = new FilterTranslation(this, languageCode);
        translations.add(filterTranslation);
        return filterTranslation;
    }

    public Iterator<FilterTranslation> getTranslations() {
        return translations.iterator();
    }

    public FilterTranslation getTranslation(String languageCode) {
        return translations.stream()
                .filter(filterTranslation -> filterTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public void removeTranslation(String language) {
        FilterTranslation translation = getTranslation(language);
        if (translation != null) {
            translations.remove(translation);
        }
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }
}