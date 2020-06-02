package no.ndla.taxonomy.service;

import no.ndla.taxonomy.config.MetadataApiConfig;
import no.ndla.taxonomy.service.dtos.MetadataApiEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MetadataApiServiceImplTest {
    private MetadataApiServiceImpl metadataApiService;
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        final var metadataApiConfig = mock(MetadataApiConfig.class);
        this.restTemplate = mock(RestTemplate.class);

        when(metadataApiConfig.getServiceUrl()).thenReturn("http://metadata");

        final var threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        this.metadataApiService = new MetadataApiServiceImpl(metadataApiConfig, restTemplate, threadPoolExecutor);
    }

    @Test
    public void getMetadataByPublicId_single() {
        when(restTemplate.getForEntity(eq("http://metadata/v1/taxonomy_entities/urn:test:1"), any(Class.class))).thenAnswer(invocationOnMock -> {
            final var aim1 = mock(MetadataApiEntity.CompetenceAim.class);
            final var aim2 = mock(MetadataApiEntity.CompetenceAim.class);

            when(aim1.getCode()).thenReturn("A1");
            when(aim2.getCode()).thenReturn("A2");

            final var returnedEntity = mock(MetadataApiEntity.class);
            when(returnedEntity.getPublicId()).thenReturn("urn:test:1");
            when(returnedEntity.getCompetenceAims()).thenReturn(Optional.of(Set.of(aim1, aim2)));

            return ResponseEntity.ok(returnedEntity);
        });

        final var returnedEntity = metadataApiService.getMetadataByPublicId(URI.create("urn:test:1"));
        assertEquals(2, returnedEntity.getGrepCodes().size());
        assertTrue(returnedEntity.getGrepCodes().containsAll(Set.of("A1", "A2")));

    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void getMetadataByPublicId_multiple() {
        final var idList = new HashSet<URI>();

        for (var i = 0; i < 1000; i++) {
            idList.add(URI.create("urn:test:" + i));
        }

        when(restTemplate.getForEntity(any(String.class), eq(MetadataApiEntity[].class))).thenAnswer(invocationOnMock -> {
            final var requestUrl = (String) invocationOnMock.getArgument(0);

            final var parsedUrl = URI.create(requestUrl);

            assertEquals("/v1/taxonomy_entities/", parsedUrl.getPath());
            assertEquals("metadata", parsedUrl.getHost());
            assertEquals("http", parsedUrl.getScheme());

            final var queryMap = UriComponentsBuilder.fromUriString(requestUrl).build().getQueryParams();

            assertTrue(queryMap.containsKey("publicIds"));
            final var publicIdListString = queryMap.getFirst("publicIds");

            final var publicIdList = Arrays.stream(publicIdListString.split(",")).map(URI::create).collect(Collectors.toSet());

            assertTrue(publicIdList.size() <= 100);
            assertTrue(publicIdList.size() > 0);

            assertTrue(idList.containsAll(publicIdList));

            final var entitiesToReturn = new ArrayList<MetadataApiEntity>();


            publicIdList.forEach(publicId -> {
                final var entityMock = mock(MetadataApiEntity.class);
                when(entityMock.getPublicId()).thenReturn(publicId.toString());

                final var aim = mock(MetadataApiEntity.CompetenceAim.class);
                when(aim.getCode()).thenReturn(publicId.getSchemeSpecificPart().toUpperCase().replace(":", ""));

                when(entityMock.getCompetenceAims()).thenReturn(Optional.of(Set.of(aim)));
                when(entityMock.isVisible()).thenReturn(Optional.of(false));

                entitiesToReturn.add(entityMock);
            });

            return ResponseEntity.ok(entitiesToReturn.toArray(new MetadataApiEntity[0]));
        });

        final var returned = metadataApiService.getMetadataByPublicId(idList);

        assertEquals(1000, returned.size());

        returned.forEach(metadataDto -> {
            final var publicId = metadataDto.getPublicId();
            assertFalse(metadataDto.isVisible());
            assertEquals(1, metadataDto.getGrepCodes().size());
            assertTrue(metadataDto.getGrepCodes().contains(URI.create(publicId).getSchemeSpecificPart().toUpperCase().replace(":", "")));

            assertTrue(idList.contains(URI.create(publicId)));
        });
    }


    @Test
    public void updateMetadataByPublicId() {
        doAnswer(invocationOnMock -> {
            final var apiEntity = (MetadataApiEntity) invocationOnMock.getArgument(1);

            assertEquals(2, apiEntity.getCompetenceAims().orElseThrow().size());
            assertTrue(
                    apiEntity.getCompetenceAims()
                            .stream()
                            .flatMap(Set::stream)
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
            when(returnedEntity.getCompetenceAims()).thenReturn(Optional.of(Set.of(aim1, aim2)));

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
    public void deleteMetadataByPublicId() {
        doThrow(new RestClientException("")).when(restTemplate).delete("http://metadata/v1/taxonomy_entities/urn:test:124");

        metadataApiService.deleteMetadataByPublicId(URI.create("urn:test:123"));
        verify(restTemplate).delete("http://metadata/v1/taxonomy_entities/urn:test:123");

        try {
            metadataApiService.deleteMetadataByPublicId(URI.create("urn:test:124"));
            fail("Expected ServiceUnavailableException");
        } catch (ServiceUnavailableException ignored) {

        }
    }

    @Test
    void updateMetadataByPublicIds() {
        final var idList = new HashSet<URI>();

        for (var i = 0; i < 1000; i++) {
            idList.add(URI.create("urn:test:" + i));
        }

        final var updatedIds = new HashSet<>();

        when(restTemplate.getForEntity(any(String.class), eq(MetadataApiEntity[].class))).thenAnswer(invocationOnMock -> {
            final var requestUrl = (String) invocationOnMock.getArgument(0);

            final var parsedUrl = URI.create(requestUrl);

            assertEquals("/v1/taxonomy_entities/", parsedUrl.getPath());
            assertEquals("metadata", parsedUrl.getHost());
            assertEquals("http", parsedUrl.getScheme());

            final var queryMap = UriComponentsBuilder.fromUriString(requestUrl).build().getQueryParams();

            assertTrue(queryMap.containsKey("publicIds"));
            final var publicIdListString = queryMap.getFirst("publicIds");

            final var publicIdList = Arrays.stream(publicIdListString.split(",")).map(URI::create).collect(Collectors.toSet());

            assertTrue(publicIdList.size() <= 100);
            assertTrue(publicIdList.size() > 0);

            assertTrue(idList.containsAll(publicIdList));

            final var entitiesToReturn = new ArrayList<MetadataApiEntity>();


            publicIdList.forEach(publicId -> {
                final var entityMock = mock(MetadataApiEntity.class);
                when(entityMock.getPublicId()).thenReturn(publicId.toString());

                final var aim = mock(MetadataApiEntity.CompetenceAim.class);
                when(aim.getCode()).thenReturn(publicId.getSchemeSpecificPart().toUpperCase().replace(":", ""));

                when(entityMock.getCompetenceAims()).thenReturn(Optional.of(Set.of(aim)));
                when(entityMock.isVisible()).thenReturn(Optional.of(false));

                entitiesToReturn.add(entityMock);
            });

            return ResponseEntity.ok(entitiesToReturn.toArray(new MetadataApiEntity[0]));
        });

        doAnswer(invocationOnMock -> {
            final var requestUrl = (String) invocationOnMock.getArgument(0);
            final var requestObjects = (Set<MetadataApiEntity>) invocationOnMock.getArgument(1);

            final var parsedUrl = URI.create(requestUrl);

            assertEquals("/v1/taxonomy_entities/", parsedUrl.getPath());
            assertEquals("metadata", parsedUrl.getHost());
            assertEquals("http", parsedUrl.getScheme());

            final var publicIdList = requestObjects.stream()
                    .map(MetadataApiEntity::getPublicId)
                    .map(URI::create)
                    .collect(Collectors.toSet());

            assertTrue(publicIdList.size() <= 100);
            assertTrue(publicIdList.size() > 0);

            assertTrue(idList.containsAll(publicIdList));

            requestObjects.forEach(requestObject -> {
                final var publicId = URI.create(requestObject.getPublicId());

                assertFalse(requestObject.isVisible().orElseThrow());
                assertEquals(1, requestObject.getCompetenceAims().orElseThrow().size());
                assertTrue(requestObject.getCompetenceAims().orElseThrow().stream().map(MetadataApiEntity.CompetenceAim::getCode).collect(Collectors.toSet()).contains("T1"));

                assertFalse(updatedIds.contains(publicId));
                updatedIds.add(publicId);
            });

            return ResponseEntity.ok();
        }).when(restTemplate).put(any(String.class), anySet());

        final var requestObject = new MetadataDto();
        requestObject.setVisible(false);
        requestObject.setGrepCodes(Set.of("T1"));

        final var returned = metadataApiService.updateMetadataByPublicIds(idList, requestObject);

        assertEquals(1000, returned.size());

        returned.forEach(metadataDto -> {
            final var publicId = metadataDto.getPublicId();
            assertFalse(metadataDto.isVisible());
            assertEquals(1, metadataDto.getGrepCodes().size());
            assertTrue(metadataDto.getGrepCodes().contains(URI.create(publicId).getSchemeSpecificPart().toUpperCase().replace(":", "")));

            assertTrue(idList.contains(URI.create(publicId)));
        });

        assertEquals(1000, updatedIds.size());
    }
}