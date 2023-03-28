/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest;

import io.swagger.v3.oas.annotations.Operation;
import no.ndla.taxonomy.service.NodeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = { "/internal" })
public class InternalController {
    private final NodeService nodeService;

    public InternalController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/buildContexts")
    @Operation(summary = "Updates contexts for all roots")
    @Transactional
    @Async
    //@PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void buildAllContexts() {
        nodeService.buildAllContexts();
    }
}
