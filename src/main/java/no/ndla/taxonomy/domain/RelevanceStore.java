/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import no.ndla.taxonomy.service.dtos.TranslationDTO;

public final class RelevanceStore {
    static final RelevanceDTO core = new RelevanceDTO(
            URI.create("urn:relevance:core"),
            "Kjernestoff",
            Set.of(
                    new TranslationDTO("nb", "Kjernestoff"),
                    new TranslationDTO("nn", "Kjernestoff"),
                    new TranslationDTO("en", "Core content"),
                    new TranslationDTO("se", "Guovddášávnnas")),
            Relevance.CORE);
    static final RelevanceDTO supplementary = new RelevanceDTO(
            URI.create("urn:relevance:supplementary"),
            "Tilleggsstoff",
            Set.of(
                    new TranslationDTO("nb", "Tilleggsstoff"),
                    new TranslationDTO("nn", "Tilleggstoff"),
                    new TranslationDTO("en", "Supplementary content"),
                    new TranslationDTO("se", "Lassiávnnas")),
            Relevance.SUPPLEMENTARY);

    static final List<RelevanceDTO> relevances = List.of(core, supplementary);

    public RelevanceStore() {}

    public static RelevanceDTO fromEnum(Relevance relevance) {
        switch (relevance) {
            case CORE -> {
                return core;
            }
            case SUPPLEMENTARY -> {
                return supplementary;
            }
        }
        throw new RuntimeException("Unknown relevance: " + relevance);
    }

    public static List<RelevanceDTO> getAllRelevances(String language) {
        return relevances.stream()
                .map(relevance -> relevance.getTranslated(language))
                .toList();
    }

    public static Optional<RelevanceDTO> getRelevance(URI id) {
        return relevances.stream().filter(relevance -> relevance.id.equals(id)).findFirst();
    }

    public static RelevanceDTO unsafeGetRelevance(URI id) {
        return getRelevance(id).orElseThrow(() -> new NotFoundException("Relevance", id));
    }

    public static RelevanceDTO unsafeGetRelevance(URI id, String language) {
        return getRelevance(id)
                .map(relevance -> relevance.getTranslated(language))
                .orElseThrow(() -> new NotFoundException("Relevance", id));
    }
}
