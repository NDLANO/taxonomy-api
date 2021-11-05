/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class ResourceTypeTranslation implements Translation {
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

    ResourceTypeTranslation() {
    }

    public ResourceTypeTranslation(ResourceType resourceType, String languageCode) {
        setResourceType(resourceType);
        this.languageCode = languageCode;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        if (resourceType != this.resourceType && this.resourceType != null
                && this.resourceType.getTranslations().contains(this)) {
            this.resourceType.removeTranslation(this);
        }
        this.resourceType = resourceType;

        if (resourceType != null && !resourceType.getTranslations().contains(this)) {
            resourceType.addTranslation(this);
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
