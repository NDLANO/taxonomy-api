/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.util.HashUtil;

import javax.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema="taxonomy_api")
public class Version extends DomainEntity {
    @Column
    @Enumerated(EnumType.STRING)
    private VersionType versionType = VersionType.BETA;

    @Column
    private String name;

    @Column
    private final String hash;

    @Column
    private Instant published;

    @Column
    private Instant archived;

    public Version() {
        setPublicId(URI.create("urn:version:" + UUID.randomUUID()));
        this.hash = HashUtil.shortHash(getPublicId());
    }

    public VersionType getVersionType() {
        return versionType;
    }

    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public Instant getPublished() {
        return published;
    }

    public void setPublished(Instant published) {
        this.published = published;
    }

    public Instant getArchived() {
        return archived;
    }

    public void setArchived(Instant archived) {
        this.archived = archived;
    }
}
