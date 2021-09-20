package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.SubjectTopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@Transactional
@RequestMapping(path = {"/v1/subjects"})
public class Subjects extends CrudControllerWithMetadata<Subject> {
    private final SubjectRepository subjectRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicTreeSorter topicTreeSorter;
    private final SubjectService subjectService;
    private final RecursiveTopicTreeService recursiveTopicTreeService;
    private final ResourceService resourceService;

    public Subjects(SubjectRepository subjectRepository,
                    SubjectTopicRepository subjectTopicRepository,
                    TopicSubtopicRepository topicSubtopicRepository,
                    TopicTreeSorter topicTreeSorter,
                    SubjectService subjectService,
                    CachedUrlUpdaterService cachedUrlUpdaterService,
                    RecursiveTopicTreeService recursiveTopicTreeService,
                    ResourceService resourceService,
                    MetadataApiService metadataApiService,
                    MetadataUpdateService metadataUpdateService
    ) {
        super(subjectRepository, cachedUrlUpdaterService, metadataApiService, metadataUpdateService);

        this.subjectRepository = subjectRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicTreeSorter = topicTreeSorter;
        this.subjectService = subjectService;
        this.recursiveTopicTreeService = recursiveTopicTreeService;
        this.resourceService = resourceService;
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    @InjectMetadata
    public List<SubjectIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Filter by key and value")
            @RequestParam(value = "key", required = false)
                    String key,

            @ApiParam(value = "Fitler by key and value")
            @RequestParam(value = "value", required = false)
                    String value
    ) {
        if (key != null) {
            return subjectService.getSubjects(language, new MetadataKeyValueQuery(key, value));
        }

        return subjectService.getSubjects(language);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single subject", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    @InjectMetadata
    public SubjectIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return subjectRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .map(subject -> new SubjectIndexDocument(subject, language))
                .orElseThrow(() -> new NotFoundHttpResponseException("Subject not found"));
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject. Fields not included will be set to null.") @RequestBody SubjectCommand command
    ) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "subject", value = "The new subject") @RequestBody SubjectCommand command) {
        final var subject = new Subject();
        return doPost(subject, command);
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all topics associated with a subject", notes = "This resource is read-only. To update the relationship between subjects and topics, use the resource /subject-topics.")
    @InjectMetadata
    public List<SubTopicIndexDocument> getTopics(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all topics will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    Set<URI> filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) {
        final var subject = subjectRepository.findFirstByPublicId(id)
                .orElseThrow(() -> new NotFoundException("Subject", id));

        final List<Integer> topicIds;

        if (recursive) {
            topicIds = recursiveTopicTreeService.getRecursiveTopics(subject)
                    .stream()
                    .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                    .collect(Collectors.toList());
        } else {
            topicIds = subject.getSubjectTopics().stream()
                    .map(SubjectTopic::getTopic)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Topic::getId)
                    .collect(Collectors.toList());
        }

        final Set<URI> filterIdSet = new HashSet<>(filterIds);

        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final var subjectTopics = subjectTopicRepository.findAllBySubjectAndTopicId(subject, topicIds);
        final var topicSubtopics = topicSubtopicRepository.findAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(topicIds);

        final var returnList = new ArrayList<SubTopicIndexDocument>();

        // Filtering

        final var filteredSubjectTopics = subjectTopics.stream()
                .filter(subjectTopic -> subjectTopic.getTopic().isPresent())
                .filter(subjectTopic -> searchForFilterOrRelevance(subjectTopic, filterIdSet, relevanceArgument, topicSubtopics))
                .collect(Collectors.toList());

        final var filteredTopicSubtopics = topicSubtopics.stream()
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().isPresent())
                .filter(topicSubtopic -> searchForFilterOrRelevance(topicSubtopic, filterIdSet, relevanceArgument, topicSubtopics))
                .collect(Collectors.toList());

        // Wrapping with metadata from API if asked for

        filteredSubjectTopics.stream()
                .map(subjectTopic -> createSubTopicIndexDocument(subject, subjectTopic, language))
                .forEach(returnList::add);

        filteredTopicSubtopics.stream()
                .map(topicSubtopic -> createSubTopicIndexDocument(subject, topicSubtopic, language))
                .forEach(returnList::add);

        // Remove duplicates from the list
        // List is sorted by parent, so we assume that any subtree that has a duplicate parent also are repeated with the same subtree
        // on the duplicates, so starting to remove duplicates should not leave any parent-less

        // (Don't know how much it makes sense to sort the list by parent and rank when duplicates are removed, but old code did)
        return topicTreeSorter.sortList(returnList).stream().distinct().collect(Collectors.toList());
    }

    private SubTopicIndexDocument createSubTopicIndexDocument(Subject subject, DomainEntity connection, String language) {
        return new SubTopicIndexDocument(subject, connection, language);
    }

    private boolean hasFilterAndRelevanceOrJustFilterIfRelevanceIsNotSet(Topic topic, Collection<URI> filterPublicId, URI relevancePublicId) {
        return false;
    }

    private boolean searchForFilterOrRelevance(Object connection, Collection<URI> filterPublicId, URI relevancePublicId, Collection<TopicSubtopic> topicSubtopics) {
        if (filterPublicId.size() == 0 && relevancePublicId == null) {
            return true;
        }

        final var foundFilter = new AtomicBoolean(false);
        if (connection instanceof TopicSubtopic) {
            final var topicSubtopic = (TopicSubtopic) connection;

            topicSubtopic.getSubtopic().ifPresent(subtopic -> {
                if (hasFilterAndRelevanceOrJustFilterIfRelevanceIsNotSet(subtopic, filterPublicId, relevancePublicId)) {
                    foundFilter.set(true);
                }
            });

            topicSubtopics.stream()
                    .filter(ts -> ts.getTopic().isPresent() && ts.getSubtopic().isPresent())
                    .forEach(topicSubtopicI -> {
                        if (topicSubtopicI.getTopic().get().getId().equals(topicSubtopic.getSubtopic().get().getId())) {
                            if (searchForFilterOrRelevance(topicSubtopicI, filterPublicId, relevancePublicId, topicSubtopics)) {
                                foundFilter.set(true);
                            }
                        }
                    });
        } else if (connection instanceof SubjectTopic) {
            Topic topic = ((SubjectTopic) connection).getTopic().orElse(null);

            if (topic != null) {
                if (hasFilterAndRelevanceOrJustFilterIfRelevanceIsNotSet(topic, filterPublicId, relevancePublicId)) {
                    foundFilter.set(true);
                }

                topicSubtopics.stream()
                        .filter(st -> st.getTopic().isPresent())
                        .forEach(topicSubtopic -> {
                            if (topicSubtopic.getTopic().get().getId().equals(topic.getId())) {
                                if (searchForFilterOrRelevance(topicSubtopic, filterPublicId, relevancePublicId, topicSubtopics)) {
                                    foundFilter.set(true);
                                }
                            }
                        });
            }
        } else {
            throw new IllegalArgumentException();
        }

        return foundFilter.get();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        subjectService.delete(id);
    }


    @GetMapping("/{subjectId}/resources")
    @ApiOperation(value = "Gets all resources for a subject. Searches recursively in all topics belonging to this subject." +
            "The ordering of resources will be based on the rank of resources relative to the topics they belong to.",
            tags = {"subjects"})
    public List<ResourceWithTopicConnectionDTO> getResourcesForSubject(
            @PathVariable("subjectId") URI subjectId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) {
        final Set<URI> filterIdSet = filterIds != null ? Set.of(filterIds) : Set.of();
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        if (filterIdSet.isEmpty()) {
            return resourceService.getResourcesBySubjectId(subjectId, resourceTypeIdSet, relevanceArgument, language);
        } else {
            return List.of(); // We don't have filters.
        }
    }

}
