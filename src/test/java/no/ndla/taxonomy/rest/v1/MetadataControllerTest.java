package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.MetadataUpdateService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MetadataControllerTest {

    private MetadataApiService metadataApiService;
    private MetadataUpdateService metadataUpdateService;
    private MetadataController controller;

    @BeforeEach
    public void setUp() {
        metadataApiService = mock(MetadataApiService.class);
        metadataUpdateService = mock(MetadataUpdateService.class);
        when(metadataUpdateService.getMetadataApiService()).thenReturn(metadataApiService);

        controller = new MetadataController(metadataApiService, metadataUpdateService);
    }

    @Test
    public void getMetadata() {
        when(metadataApiService.getMetadataByPublicId(URI.create("urn:test:1"))).thenAnswer(invocationOnMock -> {
            final var toReturn = mock(MetadataDto.class);
            when(toReturn.getGrepCodes()).thenReturn(Set.of("A", "B"));

            return toReturn;
        });
        final var result = controller.getMetadata(URI.create("urn:test:1"));

        assertEquals(2, result.getGrepCodes().size());
        assertTrue(result.getGrepCodes().containsAll(Set.of("A", "B")));
    }

    @Test
    public void putMetadata() {
        when(metadataApiService.updateMetadataByPublicId(eq(URI.create(("urn:test:1"))), any(MetadataDto.class))).thenAnswer(invocationOnMock -> {
            final var toReturn = mock(MetadataDto.class);
            when(toReturn.getGrepCodes()).thenReturn(Set.of("A", "B"));

            return toReturn;
        });

        final var requestObject = new MetadataDto();
        requestObject.setGrepCodes(Set.of("C", "D"));

        final var result = controller.putMetadata(URI.create("urn:test:1"), requestObject);

        assertEquals(2, result.getGrepCodes().size());
        assertTrue(result.getGrepCodes().containsAll(Set.of("A", "B")));

        verify(metadataApiService).updateMetadataByPublicId(eq(URI.create("urn:test:1")), any(MetadataDto.class));
    }

    @Test
    void updateRecursively() {
        {
            final var returnObject = mock(RecursiveMergeResultDto.class);
            final var entityToUpdate = mock(MetadataDto.class);

            when(metadataUpdateService.updateMetadataRecursivelyByPublicId(eq(URI.create("urn:test:1")), same(entityToUpdate), eq(true))).thenReturn(returnObject);
            assertSame(returnObject, controller.updateRecursively(URI.create("urn:test:1"), true, entityToUpdate));
        }

        {
            final var returnObject = mock(RecursiveMergeResultDto.class);
            final var entityToUpdate = mock(MetadataDto.class);

            when(metadataUpdateService.updateMetadataRecursivelyByPublicId(eq(URI.create("urn:test:2")), same(entityToUpdate), eq(false))).thenReturn(returnObject);

            assertSame(returnObject, controller.updateRecursively(URI.create("urn:test:2"), false, entityToUpdate));
        }
    }
}