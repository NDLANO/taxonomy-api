/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Version extends DomainEntity {
    @Column
    @Enumerated(EnumType.STRING)
    private VersionType versionType = VersionType.BETA;

    @Column
    @CreatedDate
    private Instant created;

    @Column
    private Instant published;

    @Column
    private Instant archived;

    public Version() {
        created = Instant.now();
        setPublicId(URI.create("urn:version:" + UUID.randomUUID()));
    }

    public VersionType getVersionType() {
        return versionType;
    }

    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }

    public Instant getCreated() {
        return created;
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
