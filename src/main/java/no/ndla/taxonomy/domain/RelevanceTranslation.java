/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class RelevanceTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    @Column
    private String name;

    @Column
    private String languageCode;

    RelevanceTranslation() {
    }

    public RelevanceTranslation(Relevance relevance, String languageCode) {
        setRelevance(relevance);
        this.languageCode = languageCode;
    }

    public Relevance getRelevance() {
        return relevance;
    }

    public void setRelevance(Relevance relevance) {
        if (relevance != this.relevance && this.relevance != null && this.relevance.getTranslations().contains(this)) {
            this.relevance.removeTranslation(this);
        }
        this.relevance = relevance;

        if (relevance != null && !relevance.getTranslations().contains(this)) {
            relevance.addTranslation(this);
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
