/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.NodeResourceRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.task.ResourceFetcher;
import no.ndla.taxonomy.service.task.ResourceUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ResourceServiceImplTest {
    private Builder builder;

    private EntityConnectionService connectionService;
    private ResourceServiceImpl resourceService;

    @BeforeEach
    public void setUp(@Autowired ResourceRepository resourceRepository, @Autowired Builder builder) {
        this.builder = builder;

        connectionService = mock(EntityConnectionService.class);

        final var domainEntityHelperService = mock(DomainEntityHelperService.class);
        final var recursiveNodeTreeService = mock(RecursiveNodeTreeService.class);
        final var nodeResourceRepository = mock(NodeResourceRepository.class);
        final var versionService = mock(VersionService.class);
        final var treeSorter = mock(TreeSorter.class);
        final var resourceFetcher = mock(ResourceFetcher.class);
        final var resourceUpdater = mock(ResourceUpdater.class);

        resourceService = new ResourceServiceImpl(resourceRepository, connectionService, domainEntityHelperService,
                nodeResourceRepository, recursiveNodeTreeService, treeSorter, versionService, resourceFetcher,
                resourceUpdater);
    }

    @Test
    @Transactional
    public void delete() {
        final var createdResource = builder.resource();

        resourceService.delete(createdResource.getPublicId());

        verify(connectionService).disconnectAllChildren(createdResource);
    }
}
