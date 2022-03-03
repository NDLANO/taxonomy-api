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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
public class GrepCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String code;

    @UpdateTimestamp
    private Instant updatedAt;

    @CreationTimestamp
    private Instant createdAt;

    @ManyToMany(mappedBy = "grepCodes")
    private Set<Metadata> metadata = new HashSet<>();

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    void addMetadata(Metadata metadata) {
        this.metadata.add(metadata);
    }

    void removeMetadata(Metadata metadata) {
        this.metadata.remove(metadata);
    }

    public Set<Metadata> getMetadata() {
        return metadata.stream().collect(Collectors.toUnmodifiableSet());
    }

    boolean containsMetadata(Metadata metadata) {
        return this.metadata.contains(metadata);
    }
}
