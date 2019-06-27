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

    FilterTranslation() {
    }

    public FilterTranslation(Filter filter, String languageCode) {
        this.languageCode = languageCode;
        this.setFilter(filter);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        if (filter != this.filter && this.filter != null && this.filter.getTranslations().contains(this)) {
            this.filter.removeTranslation(this);
        }
        this.filter = filter;

        if (filter != null && !filter.getTranslations().contains(this)) {
            filter.addTranslation(this);
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
