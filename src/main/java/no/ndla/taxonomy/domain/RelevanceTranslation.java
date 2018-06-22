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

    private RelevanceTranslation() {
    }

    public RelevanceTranslation(Relevance relevance, String languageCode) {
        this.relevance = relevance;
        this.languageCode = languageCode;
    }

    public Relevance getRelevance() {
        return relevance;
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
