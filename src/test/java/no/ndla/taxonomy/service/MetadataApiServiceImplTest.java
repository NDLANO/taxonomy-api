package no.ndla.taxonomy.service;

import no.ndla.taxonomy.config.MetadataApiConfig;
import no.ndla.taxonomy.domain.MetadataApiEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MetadataApiServiceImplTest {
    private MetadataApiServiceImpl metadataApiService;
    private RestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
        final var metadataApiConfig = mock(MetadataApiConfig.class);
        this.restTemplate = mock(RestTemplate.class);

        when(metadataApiConfig.getServiceUrl()).thenReturn("http://metadata");

        this.metadataApiService = new MetadataApiServiceImpl(metadataApiConfig, restTemplate);
    }

    @Test
    public void getMetadataByPublicId() throws ServiceUnavailableException {
        when(restTemplate.getForEntity(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(Class.class))).thenAnswer(invocationOnMock -> {
            final var aim1 = mock(MetadataApiEntity.CompetenceAim.class);
            final var aim2 = mock(MetadataApiEntity.CompetenceAim.class);

            when(aim1.getCode()).thenReturn("A1");
            when(aim2.getCode()).thenReturn("A2");

            final var returnedEntity = mock(MetadataApiEntity.class);
            when(returnedEntity.getPublicId()).thenReturn("urn:test:1");
            when(returnedEntity.getCompetenceAims()).thenReturn(Set.of(aim1, aim2));

            return ResponseEntity.ok(returnedEntity);
        });

        final var returnedEntity = metadataApiService.getMetadataByPublicId(URI.create("urn:test:1"));
        assertEquals(2, returnedEntity.getGrepCodes().size());
        assertTrue(returnedEntity.getGrepCodes().containsAll(Set.of("A1", "A2")));

    }

    @Test
    public void updateMetadataByPublicId() throws ServiceUnavailableException {
        doAnswer(invocationOnMock -> {
            final var apiEntity = (MetadataApiEntity) invocationOnMock.getArgument(1);

            assertEquals(2, apiEntity.getCompetenceAims().size());
            assertTrue(
                    apiEntity.getCompetenceAims()
                            .stream()
                            .map(MetadataApiEntity.CompetenceAim::getCode)
                            .collect(Collectors.toSet())
                            .containsAll(Set.of("B1", "B2"))
            );

            return null;
        }).when(restTemplate).put(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(MetadataApiEntity.class));

        when(restTemplate.getForEntity(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(Class.class))).thenAnswer(invocationOnMock -> {
            final var aim1 = mock(MetadataApiEntity.CompetenceAim.class);
            final var aim2 = mock(MetadataApiEntity.CompetenceAim.class);

            when(aim1.getCode()).thenReturn("A3");
            when(aim2.getCode()).thenReturn("A4");

            final var returnedEntity = mock(MetadataApiEntity.class);
            when(returnedEntity.getPublicId()).thenReturn("urn:test:1");
            when(returnedEntity.getCompetenceAims()).thenReturn(Set.of(aim1, aim2));

            return ResponseEntity.ok(returnedEntity);
        });

        final var requestObject = mock(MetadataDto.class);
        when(requestObject.getGrepCodes()).thenReturn(Set.of("B1", "B2"));

        final var returnedEntity = metadataApiService.updateMetadataByPublicId(URI.create("urn:test:1"), requestObject);

        assertEquals(2, returnedEntity.getGrepCodes().size());
        assertTrue(returnedEntity.getGrepCodes().containsAll(Set.of("A3", "A4")));

        verify(restTemplate).put(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(MetadataApiEntity.class));
    }

    @Test
    public void updateMetadataByPublicId_withServiceError() {
        final var requestObject = mock(MetadataDto.class);
        when(requestObject.getGrepCodes()).thenReturn(Set.of("B1", "B2"));

        doThrow(new RestClientException("")).when(restTemplate).put(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(MetadataApiEntity.class));

        try {
            metadataApiService.updateMetadataByPublicId(URI.create("urn:test:1"), requestObject);
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ignored) {

        }
    }

    @Test
    public void getMetadataByPublicId_withServiceError() {
        when(restTemplate.getForEntity(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(Class.class))).thenThrow(new RestClientException(""));

        try {
            metadataApiService.getMetadataByPublicId(URI.create("urn:test:1"));
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ignored) {

        }
    }

    @Test
    public void deleteMetadataByPublicId() throws ServiceUnavailableException {
        doThrow(new RestClientException("")).when(restTemplate).delete("http://metadata/v1/taxonomy_entities/urn:test:124");

        metadataApiService.deleteMetadataByPublicId(URI.create("urn:test:123"));
        verify(restTemplate).delete("http://metadata/v1/taxonomy_entities/urn:test:123");

        try {
            metadataApiService.deleteMetadataByPublicId(URI.create("urn:test:124"));
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ignored) {

        }
    }
}