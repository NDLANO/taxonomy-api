/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

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
        // this.metadataRepository = metadataRepository;
        this.domainEntityHelperService = domainEntityHelperService;
        this.grepCodeService = grepCodeService;
        this.customFieldService = customFieldService;
    }

    /*
     * @Override
     *
     * @Transactional(propagation = MANDATORY) public Optional<Metadata> getMetadata(String publicId) { return
     * metadataRepository.findFirstByPublicId(publicId); }
     *
     * @Override public List<Metadata> getMetadataList(Collection<String> publicIds) { if (publicIds.size() == 0) {
     * return List.of(); }
     *
     * return metadataRepository.findAllByPublicIdInIncludingGrepCodes(publicIds); }
     */

    @Override
    public MetadataDto getMetadataByPublicId(URI publicId) {
        EntityWithPath entity = domainEntityHelperService.getEntityByPublicId(publicId);
        return new MetadataDto(entity.getMetadata());
    }

    /*
     * @Override
     *
     * @Transactional(propagation = REQUIRED) public NewMetadataDto updateMetadataForPublicId(String publicId,
     * NewMetadataDto updateDto) throws InvalidPublicIdException, InvalidDataException { var metadata =
     * metadataService.getOrCreateMetadata(publicId);
     *
     * mergeMetadata(metadata, updateDto);
     *
     * metadata = metadataService.saveMetadata(metadata);
     *
     * updateCustomFields(metadata, updateDto);
     *
     * return getMetadataForPublicId(publicId); }
     */

    @Override
    public MetadataDto updateMetadataByPublicId(URI publicId, MetadataDto metadataDto) throws InvalidDataException {
        EntityWithPath entity = domainEntityHelperService.getEntityByPublicId(publicId);
        Metadata metadata = entity.getMetadata();

        mergeMetadata(metadata, metadataDto);

        return new MetadataDto(metadata);
    }

    private void mergeMetadata(Metadata metadata, MetadataDto updateDto) throws InvalidDataException {
        if (updateDto.getGrepCodes() != null) {
            mergeGrepCodes(metadata, updateDto.getGrepCodes());
        }

        if (updateDto.getCustomFields() != null) {
            updateCustomFields(metadata, updateDto);
        }

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

    /*
     * @Override
     *
     * @Transactional(propagation = MANDATORY) public Metadata getOrCreateMetadata(String publicId) { return
     * metadataRepository.findFirstByPublicId(publicId) .orElseGet(() ->
     * metadataRepository.saveAndFlush(createEmptyMetadata(publicId))); }
     *
     * @Override
     *
     * @Transactional(propagation = MANDATORY) public List<Metadata> getOrCreateMetadataList(Collection<String>
     * publicIds) { if (publicIds.size() == 0) { return List.of(); }
     *
     * final var existingEntities = getMetadataList(publicIds).stream() .collect(Collectors.toMap(metadata ->
     * metadata.getId().toString(), metadata -> metadata));
     *
     * final var entitiesToReturn = publicIds.stream() .map(publicId -> existingEntities.computeIfAbsent(publicId,
     * this::createEmptyMetadata)) .collect(Collectors.toList());
     *
     * return metadataRepository.saveAll(entitiesToReturn); }
     */
    /*
     * @Override
     * 
     * @Transactional(propagation = MANDATORY) public Metadata saveMetadata(Metadata metadata) { return
     * metadataRepository.saveAndFlush(metadata); }
     * 
     * @Override
     * 
     * @Transactional(propagation = MANDATORY) public void saveMetadataList(Collection<Metadata> metadata) {
     * metadataRepository.saveAll(metadata); metadataRepository.flush(); }
     */

    /*
     * @Override
     *
     * @Transactional public void deleteMetadata(String publicId) {
     * metadataRepository.findFirstByPublicId(publicId).ifPresent(metadataRepository::delete); }
     */
}
