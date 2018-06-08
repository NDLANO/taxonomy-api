package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class FilterTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @Column
    private String name;

    @Column
    private String languageCode;

    private FilterTranslation() {
    }

    public FilterTranslation(Filter filter, String languageCode) {
        this.filter = filter;
        this.languageCode = languageCode;
    }

    public Filter getFilter() {
        return filter;
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
