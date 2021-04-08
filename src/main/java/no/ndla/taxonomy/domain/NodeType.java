package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class NodeType extends DomainObject {

    @OneToMany(mappedBy = "nodeType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NodeTypeTranslation> translations = new HashSet<>();

    public NodeType() {
        setPublicId(URI.create("urn:nodetype:" + UUID.randomUUID()));
    }

    public Set<NodeTypeTranslation> getTranslations() {
        return translations.stream().collect(Collectors.toUnmodifiableSet());
    }

    public NodeTypeTranslation addTranslation(String languageCode) {
        NodeTypeTranslation nodeTypeTranslation = getTranslation(languageCode).orElse(null);
        if (nodeTypeTranslation != null) return nodeTypeTranslation;

        nodeTypeTranslation = new NodeTypeTranslation(this, languageCode);
        translations.add(nodeTypeTranslation);
        return nodeTypeTranslation;
    }

    public void addTranslation(NodeTypeTranslation nodeTypeTranslation) {
        this.translations.add(nodeTypeTranslation);
        if (nodeTypeTranslation.getNodeType() != this) {
            nodeTypeTranslation.setNodeType(this);
        }
    }

    public void removeTranslation(NodeTypeTranslation translation) {
        if (translation.getNodeType() == this) {
            translations.remove(translation);
            if (translation.getNodeType() == this) {
                translation.setNodeType(null);
            }
        }
    }

    public void removeTranslation(String languageCode) {
        getTranslation(languageCode).ifPresent(this::removeTranslation);
    }

    public Optional<NodeTypeTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(nodeTypeTranslation -> nodeTypeTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }
}
