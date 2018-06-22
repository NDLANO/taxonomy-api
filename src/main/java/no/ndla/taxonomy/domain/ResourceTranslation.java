package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class ResourceTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column
    private String name;

    @Column
    private String languageCode;

    private ResourceTranslation() {
    }

    public ResourceTranslation(Resource resource, String languageCode) {
        this.resource = resource;
        this.languageCode = languageCode;
    }

    public Resource getResource() {
        return resource;
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
