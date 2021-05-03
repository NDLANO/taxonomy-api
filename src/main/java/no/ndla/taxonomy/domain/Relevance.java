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
}
