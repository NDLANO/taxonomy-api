package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MetadataEntityWrapperServiceImplTest {
    private MetadataEntityWrapperService wrapperService;
    private MetadataApiService apiService;

    @Before
    public void setUp() {
        apiService = mock(MetadataApiService.class);

        wrapperService = new MetadataEntityWrapperServiceImpl(apiService);
    }

    @Test
    public void wrapEntities_withCallback() {
        when(apiService.getMetadataByPublicId(any(Collection.class))).thenAnswer(invocationOnMock -> {
            final Collection<URI> publicIds = invocationOnMock.getArgument(0);

            final var toReturn = new HashSet<MetadataDto>();

            for (var publicId : publicIds) {
                final var metadataDto = mock(MetadataDto.class);
                when(metadataDto.getPublicId()).thenReturn(publicId.toString());
                toReturn.add(metadataDto);
            }

            return toReturn;
        });

        {
            final var entity1 = new DomainEntityMock(URI.create("urn:test1:1"), URI.create("urn:test2:1"));
            final var entity2 = new DomainEntityMock(URI.create("urn:test1:2"), URI.create("urn:test2:2"));

            final var returnedWrappedEntities = wrapperService.wrapEntities(List.of(entity1, entity2), true, DomainEntityMock::getPublicId2);

            assertEquals(2, returnedWrappedEntities.size());

            // Order of returned items must be the same as the order sent in the request

            assertSame(entity1, returnedWrappedEntities.get(0).getEntity());
            assertSame(entity2, returnedWrappedEntities.get(1).getEntity());

            assertTrue(returnedWrappedEntities.get(0).getMetadata().isPresent());
            assertTrue(returnedWrappedEntities.get(1).getMetadata().isPresent());

            assertEquals("urn:test2:1", returnedWrappedEntities.get(0).getMetadata().orElseThrow().getPublicId());
            assertEquals("urn:test2:2", returnedWrappedEntities.get(1).getMetadata().orElseThrow().getPublicId());
        }
    }

    @Test
    public void wrapEntities_withoutCallback() {
        when(apiService.getMetadataByPublicId(any(Collection.class))).thenAnswer(invocationOnMock -> {
            final Collection<URI> publicIds = invocationOnMock.getArgument(0);

            final var toReturn = new HashSet<MetadataDto>();

            for (var publicId : publicIds) {
                final var metadataDto = mock(MetadataDto.class);
                when(metadataDto.getPublicId()).thenReturn(publicId.toString());
                toReturn.add(metadataDto);
            }

            return toReturn;
        });

        {
            final var entity1 = new DomainEntityMock(URI.create("urn:test1:1"), URI.create("urn:test2:1"));
            final var entity2 = new DomainEntityMock(URI.create("urn:test1:2"), URI.create("urn:test2:2"));

            final var returnedWrappedEntities = wrapperService.wrapEntities(List.of(entity1, entity2), true);
            final var returnedWrappedEntitiesWithoutMetadata = wrapperService.wrapEntities(List.of(entity1, entity2), false);

            assertEquals(2, returnedWrappedEntities.size());
            assertEquals(2, returnedWrappedEntitiesWithoutMetadata.size());

            // Order of returned items must be the same as the order sent in the request

            assertSame(entity1, returnedWrappedEntities.get(0).getEntity());
            assertSame(entity2, returnedWrappedEntities.get(1).getEntity());

            assertSame(entity1, returnedWrappedEntitiesWithoutMetadata.get(0).getEntity());
            assertSame(entity2, returnedWrappedEntitiesWithoutMetadata.get(1).getEntity());

            assertTrue(returnedWrappedEntities.get(0).getMetadata().isPresent());
            assertTrue(returnedWrappedEntities.get(1).getMetadata().isPresent());

            assertFalse(returnedWrappedEntitiesWithoutMetadata.get(0).getMetadata().isPresent());
            assertFalse(returnedWrappedEntitiesWithoutMetadata.get(1).getMetadata().isPresent());

            assertEquals("urn:test1:1", returnedWrappedEntities.get(0).getMetadata().orElseThrow().getPublicId());
            assertEquals("urn:test1:2", returnedWrappedEntities.get(1).getMetadata().orElseThrow().getPublicId());
        }

        // Test with something not returned from apiService
        {
            reset(apiService);
            when(apiService.getMetadataByPublicId(any(Collection.class))).thenReturn(Set.of());

            final var entity1 = new DomainEntityMock(URI.create("urn:test1:3"), URI.create("urn:test2:3"));

            final var returnedWrappedEntities = wrapperService.wrapEntities(List.of(entity1), true);

            assertEquals(1, returnedWrappedEntities.size());

            assertSame(entity1, returnedWrappedEntities.get(0).getEntity());

            assertFalse(returnedWrappedEntities.get(0).getMetadata().isPresent());
        }
    }

    @Test
    public void wrapEntity() {
        final var entityToWrap = mock(DomainEntity.class);
        when(entityToWrap.getPublicId()).thenReturn(URI.create("urn:test:1"));

        final var withoutMetadata = wrapperService.wrapEntity(entityToWrap, false);
        assertSame(entityToWrap, withoutMetadata.getEntity());
        assertFalse(withoutMetadata.getMetadata().isPresent());


        final var metadataToReturn = mock(MetadataDto.class);
        when(metadataToReturn.getPublicId()).thenReturn("urn:test:1");
        when(apiService.getMetadataByPublicId(URI.create("urn:test:1"))).thenReturn(metadataToReturn);

        final var withMetadata = wrapperService.wrapEntity(entityToWrap, true);
        assertSame(entityToWrap, withMetadata.getEntity());
        assertSame(metadataToReturn, withMetadata.getMetadata().orElseThrow());
    }

    private static class DomainEntityMock extends DomainEntity {
        private final URI publicId1;
        private final URI publicId2;

        public DomainEntityMock(URI publicId1, URI publicId2) {
            this.publicId1 = publicId1;
            this.publicId2 = publicId2;
        }

        @Override
        public URI getPublicId() {
            return publicId1;
        }

        public URI getPublicId2() {
            return publicId2;
        }
    }
}