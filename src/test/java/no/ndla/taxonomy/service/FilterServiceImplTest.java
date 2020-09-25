package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.ResourceFilterRepository;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.dtos.FilterDTO;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class FilterServiceImplTest {
    private FilterRepository filterRepository;
    private FilterServiceImpl filterService;

    @BeforeEach
    void setUp(@Autowired SubjectRepository subjectRepository,
               @Autowired FilterRepository filterRepository,
               @Autowired ResourceRepository resourceRepository,
               @Autowired ResourceFilterRepository resourceFilterRepository,
               @Autowired Builder builder) {

        this.filterRepository = filterRepository;
        final var metadataEntityWrapperService = mock(MetadataEntityWrapperService.class);

        filterService = new FilterServiceImpl(subjectRepository, filterRepository, resourceRepository, resourceFilterRepository, metadataEntityWrapperService);

        filterRepository.deleteAllAndFlush();

        final var subject = builder.subject(subjectBuilder -> {
            subjectBuilder.publicId("urn:subject:filtertest:1");
        });

        final var filter1 = builder.filter(filterBuilder -> {
            filterBuilder.contentUri(URI.create("urn:article:1"));
            filterBuilder.publicId("urn:filter:1");
            filterBuilder.name("Filter 1");
            filterBuilder.translation("nb", filterTranslationBuilder -> filterTranslationBuilder.name("Filter 1 nb"));
            filterBuilder.translation("nn", filterTranslationBuilder -> filterTranslationBuilder.name("Filter 1 nn"));
            filterBuilder.subject(subject);
        });

        final var filter2 = builder.filter(filterBuilder -> {
            filterBuilder.contentUri(URI.create("urn:article:2"));
            filterBuilder.publicId("urn:filter:2");
            filterBuilder.name("Filter 2");
            filterBuilder.translation("nb", filterTranslationBuilder -> filterTranslationBuilder.name("Filter 2 nb"));
            filterBuilder.subject(subject);
        });

        builder.filter(filterBuilder -> {
            filterBuilder.publicId("urn:filter:3");
            filterBuilder.name("Filter 3");
            filterBuilder.subject(subject);
        });

        final var relevance = builder.relevance(relevanceBuilder -> {
            relevanceBuilder.publicId("urn:relevance:filtertest:1");
        });

        builder.resource(resourceBuilder -> {
            resourceBuilder.publicId("urn:resource:filtertest:1");
            resourceBuilder.filter(filter1, relevance);
        });
        builder.resource(resourceBuilder -> {
            resourceBuilder.publicId("urn:resource:filtertest:2");
            resourceBuilder.filter(filter2, relevance);
        });

        when(metadataEntityWrapperService.wrapEntity(any(), anyBoolean())).thenAnswer(i -> metadataEntityWrapperService.wrapEntities(List.of((DomainEntity) i.getArgument(0)), i.getArgument(1)).get(0));

        when(metadataEntityWrapperService.wrapEntities(anyList(), anyBoolean()))
                .thenAnswer(invocationOnMock -> metadataEntityWrapperService.wrapEntities(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1), DomainEntity::getPublicId));

        when(metadataEntityWrapperService.wrapEntities(anyList(), anyBoolean(), any(Function.class))).thenAnswer(invocationOnMock -> {
            final var entityList = (List<Object>) invocationOnMock.getArgument(0);
            final var includeMetadata = (boolean) invocationOnMock.getArgument(1);
            final var idMapper = (Function<Object, URI>) invocationOnMock.getArgument(2);

            return entityList.stream()
                    .map(filter -> {
                        if (!includeMetadata) {
                            return new MetadataWrappedEntity<>((DomainEntity) filter, null);
                        }

                        final var metadata = mock(MetadataDto.class);

                        final var publicId = idMapper.apply(filter);

                        switch (publicId.toString()) {
                            case "urn:filter:1":
                                when(metadata.getGrepCodes()).thenReturn(Set.of("K1"));
                                when(metadata.isVisible()).thenReturn(true);
                                break;
                            case "urn:filter:2":
                                when(metadata.getGrepCodes()).thenReturn(Set.of("K2", "K3"));
                                when(metadata.isVisible()).thenReturn(false);
                                break;
                            case "urn:filter:3":
                                when(metadata.getGrepCodes()).thenReturn(Set.of());
                                when(metadata.isVisible()).thenReturn(true);
                                break;
                        }
                        return new MetadataWrappedEntity<>((DomainEntity) filter, metadata);
                    })
                    .collect(Collectors.toList());
        });
    }

    @AfterEach
    void setDown() {
        filterRepository.deleteAllAndFlush();
    }

    @Test
    void getFilters() {
        final var defaultLanguageWithoutMetadataList = filterService.getFilters(null, false);
        final var defaultLanguageWithMetadataList = filterService.getFilters(null, true);
        final var nbLanguageWithoutMetadataList = filterService.getFilters("nb", false);
        final var nnLanguageWithoutMetadataList = filterService.getFilters("nn", false);
        final var nbLanguageWithMetadataList = filterService.getFilters("nb", true);
        final var nnLanguageWithMetadataList = filterService.getFilters("nn", true);

        final var allLists = Set.of(defaultLanguageWithoutMetadataList, defaultLanguageWithMetadataList, nbLanguageWithoutMetadataList,
                nnLanguageWithoutMetadataList, nbLanguageWithMetadataList, nnLanguageWithMetadataList);

        final var withMetadataLists = Set.of(defaultLanguageWithMetadataList, nbLanguageWithMetadataList, nnLanguageWithMetadataList);
        final var nbTranslatedLists = Set.of(nbLanguageWithoutMetadataList, nbLanguageWithMetadataList);
        final var nnTranslatedLists = Set.of(nnLanguageWithoutMetadataList, nnLanguageWithMetadataList);

        for (var list : allLists) {
            assertEquals(3, list.size());

            assertTrue(
                    list.stream()
                            .map(FilterDTO::getId)
                            .map(Optional::orElseThrow)
                            .map(URI::toString)
                            .collect(Collectors.toList())
                            .containsAll(Set.of("urn:filter:1", "urn:filter:2", "urn:filter:3"))
            );

            for (var filterDto : list) {
                final var nbTranslated = nbTranslatedLists.contains(list);
                final var nnTranslated = nnTranslatedLists.contains(list);
                final var hasMetadata = withMetadataLists.contains(list);

                if (!hasMetadata) {
                    assertNull(filterDto.getMetadata());
                } else {
                    assertNotNull(filterDto.getMetadata());
                }

                assertEquals("urn:subject:filtertest:1", filterDto.getSubjectId().toString());

                switch (filterDto.getId().orElseThrow().toString()) {
                    case "urn:filter:1":
                        assertEquals(URI.create("urn:article:1"), filterDto.getContentUri());

                        if (nnTranslated) {
                            assertEquals("Filter 1 nn", filterDto.getName());
                        } else if (nbTranslated) {
                            assertEquals("Filter 1 nb", filterDto.getName());
                        } else {
                            assertEquals("Filter 1", filterDto.getName());
                        }

                        if (hasMetadata) {
                            assertTrue(filterDto.getMetadata().isVisible());
                            assertEquals(1, filterDto.getMetadata().getGrepCodes().size());
                            assertTrue(filterDto.getMetadata().getGrepCodes().contains("K1"));
                        }

                        break;
                    case "urn:filter:2":
                        assertEquals(URI.create("urn:article:2"), filterDto.getContentUri());

                        if (nbTranslated) {
                            assertEquals("Filter 2 nb", filterDto.getName());
                        } else {
                            assertEquals("Filter 2", filterDto.getName());
                        }

                        if (hasMetadata) {
                            assertFalse(filterDto.getMetadata().isVisible());
                            assertEquals(2, filterDto.getMetadata().getGrepCodes().size());
                            assertTrue(filterDto.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
                        }

                        break;
                    case "urn:filter:3":
                        assertNull(filterDto.getContentUri());
                        assertEquals("Filter 3", filterDto.getName());

                        if (hasMetadata) {
                            assertEquals(0, filterDto.getMetadata().getGrepCodes().size());
                            assertTrue(filterDto.getMetadata().isVisible());
                        }

                        break;
                }
            }
        }
    }

    @Test
    void getFilterByPublicId() {
        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:1"), null, false);
            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1", filter.getName());
            assertNull(filter.getMetadata());
            assertEquals("urn:article:1", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());
        }

        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:1"), "nb", false);
            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1 nb", filter.getName());
            assertNull(filter.getMetadata());
            assertEquals("urn:article:1", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());
        }

        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:2"), null, true);
            assertEquals("urn:filter:2", filter.getId().orElseThrow().toString());
            assertEquals("Filter 2", filter.getName());
            assertNotNull(filter.getMetadata());
            assertNotNull(filter.getMetadata().getGrepCodes());
            assertEquals("urn:article:2", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());

            assertFalse(filter.getMetadata().isVisible());
            assertEquals(2, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
        }
    }

    @Test
    void getFiltersWithConnectionByResourceId() {
        {
            final var filterList = filterService.getFiltersWithConnectionByResourceId(URI.create("urn:resource:filtertest:1"), null, false);
            assertEquals(1, filterList.size());
            final var filter = filterList.get(0);

            assertNull(filter.getMetadata());
            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1", filter.getName());
            assertNotNull(filter.getConnectionId());
            assertEquals("urn:relevance:filtertest:1", filter.getRelevanceId().toString());
        }
        {
            final var filterList = filterService.getFiltersWithConnectionByResourceId(URI.create("urn:resource:filtertest:2"), "nb", true);
            assertEquals(1, filterList.size());
            final var filter = filterList.get(0);

            assertNotNull(filter.getMetadata());
            assertEquals("urn:filter:2", filter.getId().orElseThrow().toString());
            assertEquals("Filter 2 nb", filter.getName());
            assertNotNull(filter.getConnectionId());
            assertEquals("urn:relevance:filtertest:1", filter.getRelevanceId().toString());

            assertFalse(filter.getMetadata().isVisible());
            assertEquals(2, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
        }
    }

    @Test
    void getFiltersBySubjectId() {
        {
            final var filters = filterService.getFiltersBySubjectId(URI.create("urn:subject:filtertest:1"), null, false);
            assertEquals(3, filters.size());

            assertTrue(
                    filters.stream()
                            .map(FilterDTO::getId)
                            .map(Optional::orElseThrow)
                            .map(URI::toString)
                            .collect(Collectors.toList())
                            .containsAll(Set.of("urn:filter:1", "urn:filter:2", "urn:filter:3"))
            );
            assertTrue(
                    filters.stream()
                            .map(FilterDTO::getName)
                            .collect(Collectors.toList())
                            .containsAll(Set.of("Filter 1", "Filter 2", "Filter 3"))
            );

            for (var filter : filters) {
                assertNull(filter.getMetadata());
            }
        }

        {
            final var filters = filterService.getFiltersBySubjectId(URI.create("urn:subject:filtertest:1"), "nb", true);
            assertEquals(3, filters.size());

            assertTrue(
                    filters.stream()
                            .map(FilterDTO::getId)
                            .map(Optional::orElseThrow)
                            .map(URI::toString)
                            .collect(Collectors.toList())
                            .containsAll(Set.of("urn:filter:1", "urn:filter:2", "urn:filter:3"))
            );
            assertTrue(
                    filters.stream()
                            .map(FilterDTO::getName)
                            .collect(Collectors.toList())
                            .containsAll(Set.of("Filter 1 nb", "Filter 2 nb", "Filter 3"))
            );

            for (var filter : filters) {
                switch (filter.getId().orElseThrow().toString()) {
                    case "urn:filter:1":
                        assertTrue(filter.getMetadata().isVisible());
                        assertEquals(1, filter.getMetadata().getGrepCodes().size());
                        assertTrue(filter.getMetadata().getGrepCodes().contains("K1"));
                        break;
                    case "urn:filter:2":
                        assertFalse(filter.getMetadata().isVisible());
                        assertEquals(2, filter.getMetadata().getGrepCodes().size());
                        assertTrue(filter.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
                        break;
                    case "urn:filter:3":
                        assertTrue(filter.getMetadata().isVisible());
                        assertEquals(0, filter.getMetadata().getGrepCodes().size());
                        break;
                }
            }
        }
    }
}