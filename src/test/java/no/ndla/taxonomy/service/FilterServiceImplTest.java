package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.service.dtos.FilterDTO;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class FilterServiceImplTest {
    private FilterServiceImpl filterService;

    @MockBean
    private MetadataApiService metadataApiService;

    @BeforeEach
    void setUp(@Autowired FilterRepository filterRepository,
               @Autowired Builder builder,
               @Autowired FilterServiceImpl filterService) {

        this.filterService = filterService;

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

        when(metadataApiService.getMetadataByPublicId(any(URI.class))).thenAnswer(i -> metadataApiService.getMetadataByPublicId(List.of((URI) i.getArgument(0))).stream().findFirst().orElseThrow());

        when(metadataApiService.getMetadataByPublicId(anyCollection())).thenAnswer(invocationOnMock -> {
            @SuppressWarnings("unchecked") final var publicIdList = (Collection<URI>) invocationOnMock.getArgument(0);

            return publicIdList.stream()
                    .map(publicId -> {
                        final var metadata = mock(MetadataDto.class);

                        when(metadata.getPublicId()).thenReturn(publicId.toString());

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
                        return metadata;
                    })
                    .collect(Collectors.toSet());
        });
    }

    @AfterEach
    void setDown(@Autowired FilterRepository filterRepository) {
        filterRepository.deleteAllAndFlush();
    }

    @Test
    void getFilters() {
        final var defaultLanguageList = filterService.getFilters(null);
        final var nbLanguageList = filterService.getFilters("nb");
        final var nnLanguageList = filterService.getFilters("nn");

        final var allLists = Set.of(defaultLanguageList, nbLanguageList, nnLanguageList);

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
                final var nbTranslated = list == nbLanguageList;
                final var nnTranslated = list == nnLanguageList;

                assertNotNull(filterDto.getMetadata());

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

                        assertTrue(filterDto.getMetadata().isVisible());
                        assertEquals(1, filterDto.getMetadata().getGrepCodes().size());
                        assertTrue(filterDto.getMetadata().getGrepCodes().contains("K1"));

                        break;
                    case "urn:filter:2":
                        assertEquals(URI.create("urn:article:2"), filterDto.getContentUri());

                        if (nbTranslated) {
                            assertEquals("Filter 2 nb", filterDto.getName());
                        } else {
                            assertEquals("Filter 2", filterDto.getName());
                        }

                        assertFalse(filterDto.getMetadata().isVisible());
                        assertEquals(2, filterDto.getMetadata().getGrepCodes().size());
                        assertTrue(filterDto.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));

                        break;
                    case "urn:filter:3":
                        assertNull(filterDto.getContentUri());
                        assertEquals("Filter 3", filterDto.getName());

                        assertEquals(0, filterDto.getMetadata().getGrepCodes().size());
                        assertTrue(filterDto.getMetadata().isVisible());

                        break;
                }
            }
        }
    }

    @Test
    void getFilterByPublicId() {
        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:1"), null);
            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1", filter.getName());
            assertEquals("urn:article:1", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());

            assertNotNull(filter.getMetadata());
            assertTrue(filter.getMetadata().isVisible());
            assertEquals(1, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().contains("K1"));
        }

        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:1"), "nb");
            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1 nb", filter.getName());
            assertEquals("urn:article:1", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());

            assertNotNull(filter.getMetadata());
            assertTrue(filter.getMetadata().isVisible());
            assertEquals(1, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().contains("K1"));
        }

        {
            final var filter = filterService.getFilterByPublicId(URI.create("urn:filter:2"), null);
            assertEquals("urn:filter:2", filter.getId().orElseThrow().toString());
            assertEquals("Filter 2", filter.getName());
            assertEquals("urn:article:2", filter.getContentUri().toString());
            assertEquals("urn:subject:filtertest:1", filter.getSubjectId().toString());

            assertNotNull(filter.getMetadata());
            assertFalse(filter.getMetadata().isVisible());
            assertEquals(2, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
        }
    }

    @Test
    void getFiltersWithConnectionByResourceId() {
        {
            final var filterList = filterService.getFiltersWithConnectionByResourceId(URI.create("urn:resource:filtertest:1"), null);
            assertEquals(1, filterList.size());
            final var filter = filterList.get(0);

            assertEquals("urn:filter:1", filter.getId().orElseThrow().toString());
            assertEquals("Filter 1", filter.getName());
            assertNotNull(filter.getConnectionId());
            assertEquals("urn:relevance:filtertest:1", filter.getRelevanceId().toString());

            assertNotNull(filter.getMetadata());
            assertTrue(filter.getMetadata().isVisible());
            assertEquals(1, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().contains("K1"));
        }
        {
            final var filterList = filterService.getFiltersWithConnectionByResourceId(URI.create("urn:resource:filtertest:2"), "nb");
            assertEquals(1, filterList.size());
            final var filter = filterList.get(0);

            assertEquals("urn:filter:2", filter.getId().orElseThrow().toString());
            assertEquals("Filter 2 nb", filter.getName());
            assertNotNull(filter.getConnectionId());
            assertEquals("urn:relevance:filtertest:1", filter.getRelevanceId().toString());

            assertNotNull(filter.getMetadata());
            assertFalse(filter.getMetadata().isVisible());
            assertEquals(2, filter.getMetadata().getGrepCodes().size());
            assertTrue(filter.getMetadata().getGrepCodes().containsAll(Set.of("K2", "K3")));
        }
    }

    @Test
    void getFiltersBySubjectId() {
        {
            final var filters = filterService.getFiltersBySubjectId(URI.create("urn:subject:filtertest:1"), null);
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
                assertNotNull(filter.getMetadata());
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

        {
            final var filters = filterService.getFiltersBySubjectId(URI.create("urn:subject:filtertest:1"), "nb");
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
                assertNotNull(filter.getMetadata());
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