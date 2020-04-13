package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.DomainObject;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PathResolvableEntityRestControllerTest {

    private MetadataApiService metadataApiService;
    private PathResolvableEntityRestController<MockEntity> controller;

    @Before
    public void setUp() throws Exception {
        metadataApiService = mock(MetadataApiService.class);
        controller = new MockController(metadataApiService);
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

    private static class MockEntity extends DomainObject {

    }

    private static class MockController extends PathResolvableEntityRestController<MockEntity> {

        MockController(MetadataApiService metadataApiService) {
            super(metadataApiService);
        }
    }
}