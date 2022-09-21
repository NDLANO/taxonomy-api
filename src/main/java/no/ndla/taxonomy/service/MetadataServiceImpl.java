/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithMetadata;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.GrepCode;
import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
import no.ndla.taxonomy.service.exceptions.InvalidDataException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class MetadataServiceImpl implements MetadataService {
    private final DomainEntityHelperService domainEntityHelperService;
    private final GrepCodeService grepCodeService;
    private final CustomFieldService customFieldService;

    public MetadataServiceImpl(DomainEntityHelperService domainEntityHelperService, GrepCodeService grepCodeService,
            CustomFieldService customFieldService) {
        this.domainEntityHelperService = domainEntityHelperService;
        this.grepCodeService = grepCodeService;
        this.customFieldService = customFieldService;
    }

    @Override
    public MetadataDto getMetadataByPublicId(URI publicId) {
        EntityWithMetadata entity = domainEntityHelperService.getEntityByPublicId(publicId);
        return new MetadataDto(entity.getMetadata());
    }

    @Override
    public MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataDto) throws InvalidDataException {
        EntityWithMetadata entity = domainEntityHelperService.getEntityByPublicId(publicId);
        Metadata metadata = entity.getMetadata();

        mergeMetadata(metadata, metadataDto);

        return new MetadataDto(metadata);
    }

    private void mergeMetadata(Metadata metadata, MetadataDto updateDto) throws InvalidDataException {
        if (updateDto.getGrepCodes() != null) {
            mergeGrepCodes(metadata, updateDto.getGrepCodes());
        }

        updateCustomFields(metadata, updateDto);

        if (updateDto.isVisible() != null) {
            metadata.setVisible(updateDto.isVisible());
        }
    }

    private void mergeGrepCodes(Metadata metadata, Set<String> grepCodes) {
        final var newCodes = grepCodes.stream().map(String::toUpperCase).collect(Collectors.toSet());

        final var existingCodes = metadata.getGrepCodes().stream().map(GrepCode::getCode).map(String::toUpperCase)
                .collect(Collectors.toSet());

        // Add codes that does not exist
        newCodes.stream().filter(newCode -> !existingCodes.contains(newCode)).map(grepCodeService::getOrCreateGrepCode)
                .forEach(metadata::addGrepCode);

        // Remove codes not in list
        existingCodes.stream().filter(existingCode -> !newCodes.contains(existingCode))
                .map(grepCodeService::getOrCreateGrepCode).forEach(metadata::removeGrepCode);
    }

    private void updateCustomFields(final Metadata metadata, final MetadataDto updateDto) throws InvalidDataException {
        final var customFieldMap = updateDto.getCustomFields();
        if (customFieldMap != null) {
            try {
                final var existingFields = customFieldService.getCustomFields(metadata);
                final var removeFields = existingFields.entrySet().stream()
                        .filter(entry -> !customFieldMap.containsKey(entry.getKey())).map(Map.Entry::getValue)
                        .map(CustomFieldService.FieldValue::getId);
                final var setFields = customFieldMap.entrySet().stream().filter(entry -> {
                    final var existing = existingFields.get(entry.getKey());
                    if (existing == null) {
                        return true;
                    }
                    final var existingValue = existing.getValue();
                    if (existingValue == null) {
                        return entry.getValue() != null;
                    }
                    return !existingValue.equals(entry.getValue());
                }).collect(Collectors.toMap(entry -> {
                    final var key = entry.getKey();
                    if (key == null) {
                        throw new CompletionException(new InvalidDataException("Null key for key/value data"));
                    }
                    return key;
                }, entry -> {
                    final var value = entry.getValue();
                    if (value == null) {
                        throw new CompletionException(new InvalidDataException("Null value for key/value data"));
                    }
                    return value;
                }));
                setFields.forEach((key, value) -> customFieldService.setCustomField(metadata, key, value));
                removeFields.forEach(id -> {
                    try {
                        customFieldService.unsetCustomField(id);
                    } catch (EntityNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (CompletionException e) {
                final var cause = e.getCause();
                if (cause instanceof InvalidDataException) {
                    throw (InvalidDataException) cause;
                }
                throw e;
            }
        }
    }
}
