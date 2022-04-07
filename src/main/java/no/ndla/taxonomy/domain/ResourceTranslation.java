/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class ResourceTranslation implements Translation {
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

    ResourceTranslation() {
    }

    ResourceTranslation(ResourceTranslation translation, Resource resource) {
        this.name = translation.name;
        this.languageCode = translation.languageCode;
        this.resource = resource;

    }

    public ResourceTranslation(Resource resource, String languageCode) {
        setResource(resource);
        this.languageCode = languageCode;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        if (resource != this.resource && this.resource != null && this.resource.getTranslations().contains(this)) {
            this.resource.removeTranslation(this);
        }
        this.resource = resource;

        if (resource != null && !resource.getTranslations().contains(this)) {
            resource.addTranslation(this);
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
