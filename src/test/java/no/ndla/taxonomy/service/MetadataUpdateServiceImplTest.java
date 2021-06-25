package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class MetadataUpdateServiceImplTest {
    private TopicRepository topicRepository;
    private ResourceRepository resourceRepository;
    private MetadataApiService metadataApiService;
    private MetadataUpdateServiceImpl metadataUpdateService;

    @BeforeEach
    void setUp(@Autowired TopicRepository topicRepository,
               @Autowired Builder builder,
               @Autowired ResourceRepository resourceRepository) {

        metadataApiService = mock(MetadataApiService.class);

        builder.subject(sb -> {
            sb.publicId("urn:subject:mdup:1");
            sb.name("Subject 1");

            sb.topic(tb -> {
                tb.publicId("urn:topic:mdup:11");

                tb.resource(rb -> rb.publicId("urn:resource:mdup:111"));
                tb.resource(rb -> rb.publicId("urn:resource:mdup:112"));
            });
            sb.topic(tb -> {
                tb.publicId("urn:topic:mdup:12");

                tb.resource(rb -> rb.publicId("urn:resource:mdup:121"));
                tb.resource(rb -> rb.publicId("urn:resource:mdup:122"));
            });
        });

        builder.subject(sb -> {
            sb.publicId("urn:subject:mdup:2");
            sb.name("Subject 2");

            sb.topic(tb -> {
                tb.publicId("urn:topic:mdup:21");

                tb.resource(rb -> rb.publicId("urn:resource:mdup:211"));
                tb.resource(rb -> rb.publicId("urn:resource:mdup:212"));
            });
            sb.topic(tb -> {
                tb.publicId("urn:topic:mdup:22");

                tb.resource(rb -> rb.publicId("urn:resource:mdup:221"));
                tb.resource(rb -> rb.publicId("urn:resource:mdup:222"));
            });
        });

        metadataUpdateService = new MetadataUpdateServiceImpl(topicRepository, resourceRepository, metadataApiService);
    }

    @Test
    void updateMetadataRecursivelyByPublicId() {
        {
            final var metadataDto = new MetadataDto();
            metadataDto.setVisible(true);

            final var expectedUpdated = Set.of("urn:subject:mdup:1", "urn:topic:mdup:11", "urn:topic:mdup:12");
            final var actuallyUpdated = new ArrayList<>();

            doAnswer(invocation -> {
                final Set<URI> updatePublicIds = invocation.getArgument(0);
                final MetadataDto metadataObject = invocation.getArgument(1);

                assertTrue(metadataObject.isVisible());
                assertNull(metadataObject.getGrepCodes());

                updatePublicIds.forEach(publicId -> {
                    assertTrue(expectedUpdated.contains(publicId.toString()));
                    assertFalse(actuallyUpdated.contains(publicId));

                    actuallyUpdated.add(publicId);
                });

                return updatePublicIds.stream()
                        .map(publicId -> {
                            final var returnDto = new MetadataDto();
                            returnDto.setVisible(true);
                            returnDto.setGrepCodes(new HashSet<>());
                            returnDto.setPublicId(publicId.toString());

                            return returnDto;
                        })
                        .collect(Collectors.toSet());
            }).when(metadataApiService).updateMetadataByPublicIds(anySet(), any(MetadataDto.class));

            final var result = metadataUpdateService.updateMetadataRecursivelyByPublicId(URI.create("urn:subject:mdup:1"), metadataDto, false);
            assertEquals(3, result.getUpdatedCount());
            assertEquals(3, result.getUpdated().size());
            assertTrue(result.getUpdated().stream().map(URI::toString).collect(Collectors.toSet()).containsAll(expectedUpdated));
        }

        {
            final var metadataDto = new MetadataDto();
            metadataDto.setGrepCodes(Set.of("T1", "T2"));

            final var expectedUpdated = Set.of("urn:subject:mdup:2", "urn:topic:mdup:21", "urn:topic:mdup:22");
            final var actuallyUpdated = new ArrayList<>();

            doAnswer(invocation -> {
                final Set<URI> updatePublicIds = invocation.getArgument(0);
                final MetadataDto metadataObject = invocation.getArgument(1);

                assertNull(metadataObject.isVisible());
                assertTrue(metadataObject.getGrepCodes().size() == 2 && metadataObject.getGrepCodes().containsAll(Set.of("T1", "T2")));

                updatePublicIds.forEach(publicId -> {
                    assertTrue(expectedUpdated.contains(publicId.toString()));
                    assertFalse(actuallyUpdated.contains(publicId));

                    actuallyUpdated.add(publicId);
                });

                return updatePublicIds.stream()
                        .map(publicId -> {
                            final var returnDto = new MetadataDto();
                            returnDto.setVisible(true);
                            returnDto.setGrepCodes(new HashSet<>());
                            returnDto.setPublicId(publicId.toString());

                            return returnDto;
                        })
                        .collect(Collectors.toSet());
            }).when(metadataApiService).updateMetadataByPublicIds(anySet(), any(MetadataDto.class));

            final var result = metadataUpdateService.updateMetadataRecursivelyByPublicId(URI.create("urn:subject:mdup:2"), metadataDto, false);
            assertEquals(3, result.getUpdatedCount());
            assertEquals(3, result.getUpdated().size());
            assertTrue(result.getUpdated().stream().map(URI::toString).collect(Collectors.toSet()).containsAll(expectedUpdated));
        }

        {
            final var metadataDto = new MetadataDto();
            metadataDto.setGrepCodes(Set.of("T1", "T2"));

            final var expectedUpdated = Set.of("urn:subject:mdup:2", "urn:topic:mdup:21", "urn:topic:mdup:22", "urn:resource:mdup:211", "urn:resource:mdup:212", "urn:resource:mdup:221", "urn:resource:mdup:222");
            final var actuallyUpdated = new ArrayList<>();

            doAnswer(invocation -> {
                final Set<URI> updatePublicIds = invocation.getArgument(0);
                final MetadataDto metadataObject = invocation.getArgument(1);

                assertNull(metadataObject.isVisible());
                assertTrue(metadataObject.getGrepCodes().size() == 2 && metadataObject.getGrepCodes().containsAll(Set.of("T1", "T2")));

                updatePublicIds.forEach(publicId -> {
                    assertTrue(expectedUpdated.contains(publicId.toString()));
                    assertFalse(actuallyUpdated.contains(publicId));

                    actuallyUpdated.add(publicId);
                });

                return updatePublicIds.stream()
                        .map(publicId -> {
                            final var returnDto = new MetadataDto();
                            returnDto.setVisible(true);
                            returnDto.setGrepCodes(new HashSet<>());
                            returnDto.setPublicId(publicId.toString());

                            return returnDto;
                        })
                        .collect(Collectors.toSet());
            }).when(metadataApiService).updateMetadataByPublicIds(anySet(), any(MetadataDto.class));

            final var result = metadataUpdateService.updateMetadataRecursivelyByPublicId(URI.create("urn:subject:mdup:2"), metadataDto, true);
            assertEquals(7, result.getUpdatedCount());
            assertEquals(7, result.getUpdated().size());
            assertTrue(result.getUpdated().stream().map(URI::toString).collect(Collectors.toSet()).containsAll(expectedUpdated));
        }

        {
            final var metadataDto = new MetadataDto();
            metadataDto.setGrepCodes(Set.of("T1", "T2"));

            final var expectedUpdated = Set.of("urn:topic:mdup:21", "urn:resource:mdup:211", "urn:resource:mdup:212");
            final var actuallyUpdated = new ArrayList<>();

            doAnswer(invocation -> {
                final Set<URI> updatePublicIds = invocation.getArgument(0);
                final MetadataDto metadataObject = invocation.getArgument(1);

                assertNull(metadataObject.isVisible());
                assertTrue(metadataObject.getGrepCodes().size() == 2 && metadataObject.getGrepCodes().containsAll(Set.of("T1", "T2")));

                updatePublicIds.forEach(publicId -> {
                    assertTrue(expectedUpdated.contains(publicId.toString()));
                    assertFalse(actuallyUpdated.contains(publicId));

                    actuallyUpdated.add(publicId);
                });

                return updatePublicIds.stream()
                        .map(publicId -> {
                            final var returnDto = new MetadataDto();
                            returnDto.setVisible(true);
                            returnDto.setGrepCodes(new HashSet<>());
                            returnDto.setPublicId(publicId.toString());

                            return returnDto;
                        })
                        .collect(Collectors.toSet());
            }).when(metadataApiService).updateMetadataByPublicIds(anySet(), any(MetadataDto.class));

            final var result = metadataUpdateService.updateMetadataRecursivelyByPublicId(URI.create("urn:topic:mdup:21"), metadataDto, true);
            assertEquals(3, result.getUpdatedCount());
            assertEquals(3, result.getUpdated().size());
            assertTrue(result.getUpdated().stream().map(URI::toString).collect(Collectors.toSet()).containsAll(expectedUpdated));
        }

        {
            final var metadataDto = new MetadataDto();
            metadataDto.setGrepCodes(Set.of("T1", "T2"));

            final var expectedUpdated = Set.of("urn:resource:mdup:212");
            final var actuallyUpdated = new ArrayList<>();

            doAnswer(invocation -> {
                final Set<URI> updatePublicIds = invocation.getArgument(0);
                final MetadataDto metadataObject = invocation.getArgument(1);

                assertNull(metadataObject.isVisible());
                assertTrue(metadataObject.getGrepCodes().size() == 2 && metadataObject.getGrepCodes().containsAll(Set.of("T1", "T2")));

                updatePublicIds.forEach(publicId -> {
                    assertTrue(expectedUpdated.contains(publicId.toString()));
                    assertFalse(actuallyUpdated.contains(publicId));

                    actuallyUpdated.add(publicId);
                });

                return updatePublicIds.stream()
                        .map(publicId -> {
                            final var returnDto = new MetadataDto();
                            returnDto.setVisible(true);
                            returnDto.setGrepCodes(new HashSet<>());
                            returnDto.setPublicId(publicId.toString());

                            return returnDto;
                        })
                        .collect(Collectors.toSet());
            }).when(metadataApiService).updateMetadataByPublicIds(anySet(), any(MetadataDto.class));

            final var result = metadataUpdateService.updateMetadataRecursivelyByPublicId(URI.create("urn:resource:mdup:212"), metadataDto, true);
            assertEquals(1, result.getUpdatedCount());
            assertEquals(1, result.getUpdated().size());
            assertTrue(result.getUpdated().stream().map(URI::toString).collect(Collectors.toSet()).containsAll(expectedUpdated));
        }
    }
}