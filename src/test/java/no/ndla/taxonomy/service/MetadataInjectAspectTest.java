/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataInjectAspectTest {
    private MetadataApiService metadataApiService;
    private MetadataInjectAspect metadataInjectAspect;

    @BeforeEach
    public void beforeEach() {
        metadataApiService = mock(MetadataApiService.class);
        metadataInjectAspect = new MetadataInjectAspect(metadataApiService);
    }

    @Test
    public void testMetadataQueryAndInject() {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        final var metadataKeyValueQuery = new MetadataKeyValueQuery("key-v", "value-v");
        final var metadata1 = new MetadataDto();

        /* metadata aspect will match by public ID between metadata entity and api entity */
        metadata1.setPublicId("urn:topic:1");

        /*
         * injector will run the metadata query before handing off to the api handler.
         *
         * Lifecycle of metadata dtos: Then the metadata dtos are made available to the api handler, then after handler
         * returns the injector will match pairs of api-entity <-> metadata-dto and do the injection.
         */
        when(metadataApiService.getMetadataByKeyAndValue("key-v", "value-v")).thenReturn(Set.of(metadata1));

        final var topic1 = new NodeDTO(new Node(NodeType.TOPIC) {
            {
                /* metadata aspect will match by public ID between metadata entity and api entity */
                setPublicId(URI.create("urn:topic:1"));
            }
        }, null);
        /* Check that the topic dto has no metadata object */
        assertNull(topic1.getMetadata());

        /*
         * The mocked api/service handler.
         */
        List<NodeDTO> nodes = List.of(topic1);
        try {
            when(pjp.proceed()).thenReturn(nodes);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        /*
         * Try running the clockwork..
         */
        try {
            assertSame(nodes, metadataInjectAspect.metadataQueryAndInject(pjp, metadataKeyValueQuery));
        } catch (Throwable throwable) {
            assertFalse(true);
        }
        /* The metadata should have been injected */
        assertSame(metadata1, topic1.getMetadata());
    }
}
