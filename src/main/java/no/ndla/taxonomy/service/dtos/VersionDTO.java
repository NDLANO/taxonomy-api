/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.net.URI;
import java.time.Instant;

@ApiModel("Version")
public class VersionDTO {
    @JsonProperty
    @ApiModelProperty(example = "urn:version:1")
    private URI id;

    @JsonProperty
    @ApiModelProperty(example = "BETA")
    @Enumerated(EnumType.STRING)
    private VersionType versionType;

    @JsonProperty
    @ApiModelProperty(notes = "Timestamp for when version was created")
    private Instant created;

    @JsonProperty
    @ApiModelProperty(notes = "Timestamp for when version was published")
    private Instant published;

    @JsonProperty
    @ApiModelProperty(notes = "Timestamp for when version was archived")
    private Instant archived;

    public VersionDTO() {
    }

    public VersionDTO(Version version) {
        this.id = version.getPublicId();
        this.versionType = version.getVersionType();
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
