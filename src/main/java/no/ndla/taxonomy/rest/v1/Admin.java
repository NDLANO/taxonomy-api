/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.service.NodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/v1/admin"})
public class Admin {
    private final NodeService nodeService;

    public Admin(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/buildContexts")
    @Operation(
            summary = "Updates contexts for all roots. Requires taxonomy:admin access.",
            security = {@SecurityRequirement(name = "oauth")})
    @Transactional
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void buildAllContexts() {
        nodeService.buildAllContexts();
    }
}
