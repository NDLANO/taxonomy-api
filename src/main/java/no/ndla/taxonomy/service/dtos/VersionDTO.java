/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.net.URI;
import java.time.Instant;

@Schema(name = "Version")
public class VersionDTO {
    @JsonProperty
    @Schema(example = "urn:version:1")
    private URI id;

    @JsonProperty
    @Schema(example = "BETA")
    @Enumerated(EnumType.STRING)
    private VersionType versionType;

    @JsonProperty
    @Schema(description = "Name for the version")
    private String name;

    @JsonProperty
    @Schema(description = "Unique hash for the version")
    private String hash;

    @JsonProperty
    @Schema(description = "Is the version locked")
    private Boolean locked;

    @JsonProperty
    @Schema(description = "Timestamp for when version was created")
    private Instant created;

    @JsonProperty
    @Schema(description = "Timestamp for when version was published")
    private Instant published;

    @JsonProperty
    @Schema(description = "Timestamp for when version was archived")
    private Instant archived;

    public VersionDTO() {
    }

    public VersionDTO(Version version) {
        this.id = version.getPublicId();
        this.versionType = version.getVersionType();
        this.name = version.getName();
        this.hash = version.getHash();
        this.locked = version.isLocked();
        this.created = version.getCreated();
        this.published = version.getPublished();
        this.archived = version.getArchived();
    }

    public URI getId() {
        return id;
    }

    public VersionType getVersionType() {
        return versionType;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getPublished() {
        return published;
    }

    public Instant getArchived() {
        return archived;
    }
}
