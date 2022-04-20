/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class NodeTranslation implements Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private Node node;

    @Column
    private String name;

    @Column
    private String languageCode;

    public NodeTranslation() {
    }

    public NodeTranslation(NodeTranslation translation, Node node) {
        this.name = translation.name;
        this.languageCode = translation.languageCode;
        this.node = node;
    }

    public NodeTranslation(Node node, String languageCode) {
        setNode(node);
        this.languageCode = languageCode;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (node != this.node && this.node != null && this.node.getTranslations().contains(this)) {
            this.node.removeTranslation(this);
        }
        this.node = node;

        if (node != null && !node.getTranslations().contains(this)) {
            node.addTranslation(this);
        }
    }

    @Override
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
