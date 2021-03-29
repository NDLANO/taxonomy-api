package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class NodeTypeTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "node_type_id")
    private NodeType nodeType;

    @Column
    private String name;

    @Column
    private String languageCode;

    NodeTypeTranslation() {
    }

    public NodeTypeTranslation(NodeType nodeType, String languageCode) {
        setNodeType(nodeType);
        this.languageCode = languageCode;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        if (nodeType != this.nodeType && this.nodeType != null && this.nodeType.getTranslations().contains(this)) {
            this.nodeType.removeTranslation(this);
        }
        this.nodeType = nodeType;

        if (nodeType != null && !nodeType.getTranslations().contains(this)) {
            nodeType.addTranslation(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguageCode() {
        return languageCode;
    }
}
