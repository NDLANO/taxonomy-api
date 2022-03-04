/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Takes a http request and extracts a given header. Returns a database schema name.
 */
@Component
public class VersionHeaderExtractor {

    @Value("${spring.datasource.hikari.schema:public}")
    private String defaultSchema;

    private final VersionRepository versionRepository;

    public VersionHeaderExtractor(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    public String getVersionSchemaFromHeader(HttpServletRequest req) {
        String versionHash = req.getHeader("VersionHash");
        if (versionHash == null) {
            // No header, check published and use for gets
            Optional<Version> published = versionRepository.findFirstByVersionType(VersionType.PUBLISHED);
            if (published.isPresent() && req.getMethod().equals("GET")) {
                // Use published for all GETs
                return String.format("%s_%s", defaultSchema, published.get().getHash());
            }
        } else {
            // Header supplied, use that version if in database
            Optional<Version> version = versionRepository.findFirstByHash(versionHash);
            if (version.isPresent()) {
                return String.format("%s_%s", defaultSchema, version.get().getHash());
            }
        }
        // Either no header or no version matching header. Use default schema.
        return defaultSchema;
    }
}
