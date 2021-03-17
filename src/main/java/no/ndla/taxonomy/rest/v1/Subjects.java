package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicSubtopicRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.service.*;
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
public class Subjects extends CrudController<Topic> {
    private final TopicRepository topicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicTreeSorter topicTreeSorter;
    private final SubjectService subjectService;
    private final RecursiveTopicTreeService recursiveTopicTreeService;

    public Subjects(TopicRepository topicRepository,
                    TopicSubtopicRepository topicSubtopicRepository,
                    TopicTreeSorter topicTreeSorter, SubjectService subjectService,
                    CachedUrlUpdaterService cachedUrlUpdaterService, RecursiveTopicTreeService recursiveTopicTreeService) {
        super(topicRepository, cachedUrlUpdaterService);

        this.topicRepository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicTreeSorter = topicTreeSorter;
        this.subjectService = subjectService;
        this.recursiveTopicTreeService = recursiveTopicTreeService;

        this.validator = new SubjectURNValidator();
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    @InjectMetadata
    public List<SubjectIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return topicRepository.findAllIncludingCachedUrlsAndTranslations()
                .stream()
                .filter(topic -> topic.getPublicId() != null && topic.getPublicId().toString().startsWith("urn:subject:"))
                .map(subject -> new SubjectIndexDocument(subject, language))
                .collect(Collectors.toList());
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
        validator.validate(id, "subject");
        return topicRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
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
        final var subject = new Topic();
        subject.setPublicId(URI.create("urn:subject:" + UUID.randomUUID()));
        subject.setContext(true);
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
        validator.validate(id, "subject");
        final var subject = topicRepository.findFirstByPublicId(id)
                .orElseThrow(() -> new NotFoundException("Subject", id));

        final List<Integer> topicIds;

        if (recursive) {
            topicIds = recursiveTopicTreeService.getRecursiveTopics(subject)
                    .stream()
                    .map(RecursiveTopicTreeService.TopicTreeElement::getTopicId)
                    .collect(Collectors.toList());
        } else {
            topicIds = subject.getChildrenTopicSubtopics().stream()
                    .map(TopicSubtopic::getSubtopic)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Topic::getId)
                    .collect(Collectors.toList());
        }

        final Set<URI> filterIdSet = new HashSet<>(filterIds);

        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final var topicSubtopics = topicSubtopicRepository.findAllBySubtopicIdIncludeTranslationsAndCachedUrlsAndFilters(topicIds);

        final var returnList = new ArrayList<SubTopicIndexDocument>();

        // Filtering

        final var filteredTopicSubtopics = topicSubtopics.stream()
                .filter(topicSubtopic -> topicSubtopic.getSubtopic().isPresent())
                .filter(topicSubtopic -> searchForFilterOrRelevance(topicSubtopic, filterIdSet, relevanceArgument, topicSubtopics))
                .collect(Collectors.toList());

        // Wrapping with metadata from API if asked for

        filteredTopicSubtopics.stream()
                .map(topicSubtopic -> createSubTopicIndexDocument(subject, topicSubtopic, language))
                .forEach(returnList::add);

        // Remove duplicates from the list
        // List is sorted by parent, so we assume that any subtree that has a duplicate parent also are repeated with the same subtree
        // on the duplicates, so starting to remove duplicates should not leave any parent-less

        // (Don't know how much it makes sense to sort the list by parent and rank when duplicates are removed, but old code did)
        return topicTreeSorter.sortList(returnList).stream().distinct().collect(Collectors.toList());
    }

    private SubTopicIndexDocument createSubTopicIndexDocument(Topic subject, DomainEntity connection, String language) {
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
        validator.validate(id, "subject");
        subjectService.delete(id);
    }
}
