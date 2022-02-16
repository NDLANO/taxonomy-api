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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Metadata {
    @Id
    @Column
    private UUID id;

    @UpdateTimestamp
    private Instant updatedAt;

    @CreationTimestamp
    private Instant createdAt;

    @SuppressWarnings("JpaDataSourceORMInspection")
    @ManyToMany(cascade = CascadeType.ALL)
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

    public Set<GrepCode> getGrepCodes() {
        return grepCodes.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void addCustomFieldValue(CustomFieldValue customFieldValue) {
        this.customFieldValues.add(customFieldValue);
    }

    public void removeCustomFieldValue(CustomFieldValue customFieldValue) {
        this.customFieldValues.remove(customFieldValue);
    }

    public Set<CustomFieldValue> getCustomFieldValues() {
        return customFieldValues.stream().collect(Collectors.toUnmodifiableSet());
    }

    public UUID getId() {
        return id;
    }

    public String getPublicId() {
        return "";
    }

    public void setPublicId(String publicId) {

    }

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreRemove
    void preRemove() {
        // De-links the grep codes before removal (but keeps the grep code entities)
        Set.copyOf(this.grepCodes).forEach(this::removeGrepCode);
        if (customFieldValues != null) {
            customFieldValues.clear();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}