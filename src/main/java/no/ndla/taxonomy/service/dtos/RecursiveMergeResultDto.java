/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

public class RecursiveMergeResultDto {
    private int updatedCount;
    private Set<URI> updated;

    public RecursiveMergeResultDto() {
    }

    public RecursiveMergeResultDto(Set<MetadataDto> metadataDtos) {
        updatedCount = metadataDtos.size();
        updated = metadataDtos.stream().map(MetadataDto::getPublicId).map(URI::create).collect(Collectors.toSet());
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public Set<URI> getUpdated() {
        return updated;
    }

    public void setUpdated(Set<URI> updated) {
        this.updated = updated;
    }
}
