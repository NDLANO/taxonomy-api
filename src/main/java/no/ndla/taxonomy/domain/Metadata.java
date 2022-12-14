/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Metadata implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @UpdateTimestamp
    private Instant updatedAt;

    @CreationTimestamp
    private Instant createdAt;

    @SuppressWarnings("JpaDataSourceORMInspection")
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "metadata_grep_code", joinColumns = @JoinColumn(name = "metadata_id"), inverseJoinColumns = @JoinColumn(name = "grep_code_id"))
    private Set<GrepCode> grepCodes = new HashSet<>();

    // JPA will delete custom field values automatically if they are removed from this set and this
    // entity persisted.
    // This is for the @PreRemoval to take effect and automatically remove values when this entity
    // is deleted - makes sense - right? otherwise service level code would have to always delete values before
    // deleting this.
    @OneToMany(cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.REFRESH }, orphanRemoval = true, mappedBy = "metadata", fetch = FetchType.EAGER)
    private Set<CustomFieldValue> customFieldValues = new HashSet<>();

    @Column
    private boolean visible = true;

    public Metadata() {
    }

    public Metadata(Metadata metadata) {
        this.visible = metadata.isVisible();
        Set<GrepCode> gcSet = new HashSet<>();
        for (GrepCode code : metadata.getGrepCodes()) {
            gcSet.add(new GrepCode(code, this));
        }
        this.grepCodes = gcSet;
        Set<CustomFieldValue> cfvSet = new HashSet<>();
        for (CustomFieldValue customFieldValue : metadata.getCustomFieldValues()) {
            cfvSet.add(new CustomFieldValue(customFieldValue, this));
        }
        this.customFieldValues = cfvSet;
    }

    public void addGrepCode(GrepCode grepCode) {
        this.grepCodes.add(grepCode);

        if (!grepCode.containsMetadata(this)) {
            grepCode.addMetadata(this);
        }
    }

    public void removeGrepCode(GrepCode grepCode) {
        this.grepCodes.remove(grepCode);

        if (grepCode.containsMetadata(this)) {
            grepCode.removeMetadata(this);
        }
    }

    public Collection<GrepCode> getGrepCodes() {
        return grepCodes.stream().collect(Collectors.toUnmodifiableList());
    }

    public void addCustomFieldValue(CustomFieldValue customFieldValue) {
        if (customFieldValue.getMetadata() == null) {
            customFieldValue.setMetadata(this);
        }
        this.customFieldValues.add(customFieldValue);
    }

    public void removeCustomFieldValue(CustomFieldValue customFieldValue) {
        this.customFieldValues.remove(customFieldValue);
    }

    public Collection<CustomFieldValue> getCustomFieldValues() {
        return customFieldValues.stream().collect(Collectors.toUnmodifiableList());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @PreRemove
    void preRemove() {
        // De-links the grep codes before removal (but keeps the grep code entities)
        Set.copyOf(this.grepCodes).forEach(this::removeGrepCode);
        if (customFieldValues != null) {
            customFieldValues.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Metadata that = (Metadata) o;
        return visible == that.visible && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(createdAt, that.createdAt) && Objects.equals(grepCodes, that.grepCodes)
                && Objects.equals(customFieldValues, that.customFieldValues);
    }
}
