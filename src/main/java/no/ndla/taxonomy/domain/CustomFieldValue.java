/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.util.UUID;

@Entity
public class CustomFieldValue {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "metadata_id")
    private Metadata metadata;

    @ManyToOne
    @JoinColumn(name = "custom_field_id")
    private CustomField customField;

    @Column
    private String value;

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreRemove
    void preRemove() {
        this.metadata.removeCustomFieldValue(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
