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
public class Status extends DomainObject {
    @OneToMany(mappedBy = "status", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StatusTranslation> statusTranslations = new HashSet<>();

    public Status() {
        setPublicId(URI.create("urn:status:" + UUID.randomUUID()));
    }

    public StatusTranslation addTranslation(String languageCode) {
        StatusTranslation statusTranslation = getTranslation(languageCode).orElse(null);
        if (statusTranslation != null) return statusTranslation;

        statusTranslation = new StatusTranslation(this, languageCode);
        statusTranslations.add(statusTranslation);
        return statusTranslation;
    }

    public Optional<StatusTranslation> getTranslation(String languageCode) {
        return statusTranslations.stream()
                .filter(statusTranslation -> statusTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }

    public Set<StatusTranslation> getTranslations() {
        return statusTranslations.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public void addTranslation(StatusTranslation statusTranslation) {
        this.statusTranslations.add(statusTranslation);
        if (statusTranslation.getStatus() != this) {
            statusTranslation.setStatus(this);
        }
    }

    public void removeTranslation(StatusTranslation statusTranslation) {
        if (statusTranslation.getStatus() == this) {
            statusTranslations.remove(statusTranslation);
            if (statusTranslation.getStatus() == this) {
                statusTranslation.setStatus(null);
            }
        }
    }
}
