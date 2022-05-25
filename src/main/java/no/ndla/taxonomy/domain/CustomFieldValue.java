/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class CustomFieldValue implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "metadata_id")
    private Metadata metadata;

    @ManyToOne
    @JoinColumn(name = "custom_field_id")
    private CustomField customField;

    @Column
    private String value;

    @PreRemove
    void preRemove() {
        this.metadata.removeCustomFieldValue(this);
    }

    public CustomFieldValue() {
    }

    public CustomFieldValue(CustomField customField, String value) {
        this.value = value;
        this.customField = customField;
    }

    public CustomFieldValue(CustomFieldValue customFieldValue, Metadata metadata) {
        this.value = customFieldValue.getValue();
        this.customField = customFieldValue.getCustomField();
        this.metadata = metadata;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public CustomField getCustomField() {
        return customField;
    }

    public void setCustomField(CustomField customField) {
        this.customField = customField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
