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

    public Optional<NodeTypeTranslation> getTranslation(String languageCode) {
        return translations.stream()
                .filter(nodeTypeTranslation -> nodeTypeTranslation.getLanguageCode().equals(languageCode))
                .findFirst();
    }
}
