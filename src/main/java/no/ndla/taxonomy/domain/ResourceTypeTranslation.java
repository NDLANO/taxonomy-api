package no.ndla.taxonomy.domain;


import javax.persistence.*;

@Entity
public class ResourceTypeTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    @Column
    private String name;

    @Column
    private String languageCode;

    private ResourceTypeTranslation() {
    }

    public ResourceTypeTranslation(ResourceType resourceType, String languageCode) {
        this.resourceType = resourceType;
        this.languageCode = languageCode;
    }

    public ResourceType getResourceType() {
        return resourceType;
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
