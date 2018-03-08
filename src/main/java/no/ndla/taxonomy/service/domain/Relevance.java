package no.ndla.taxonomy.service.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Entity
public class Relevance extends DomainObject {
    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<RelevanceTranslation> translations = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<ResourceFilter> resources = new HashSet<>();

    @OneToMany(mappedBy = "relevance", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<TopicFilter> topics = new HashSet<>();

    public Relevance() {
        setPublicId(URI.create("urn:relevance:" + UUID.randomUUID()));
    }

    public RelevanceTranslation addTranslation(String languageCode) {
        RelevanceTranslation relevanceTranslation = getTranslation(languageCode);
        if (relevanceTranslation != null) return relevanceTranslation;

        relevanceTranslation = new RelevanceTranslation(this, languageCode);
        translations.add(relevanceTranslation);
        return relevanceTranslation;
    }

    public Iterator<RelevanceTranslation> getTranslations() {
        return translations.iterator();
    }

    public RelevanceTranslation getTranslation(String languageCode) {
        return translations.stream()
                .filter(relevanceTranslation -> relevanceTranslation.getLanguageCode().equals(languageCode))
                .findFirst()
                .orElse(null);
    }

    public void removeTranslation(String language) {
        RelevanceTranslation translation = getTranslation(language);
        if (translation != null) {
            translations.remove(translation);
        }
    }

}
