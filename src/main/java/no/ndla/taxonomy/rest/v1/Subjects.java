package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.commands.CreateSubjectCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateSubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.FilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.service.SubjectService;
import no.ndla.taxonomy.service.TopicResourceTreeSortable;
import no.ndla.taxonomy.service.TopicTreeSorter;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
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
public class Subjects extends CrudController<Subject> {
    private final SubjectRepository subjectRepository;
    private final TopicTreeBySubjectElementRepository subjectTopicTreeElementRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final TopicSubtopicRepository topicSubtopicRepository;
    private final TopicTreeSorter topicTreeSorter;
    private final SubjectService subjectService;

    public Subjects(SubjectRepository subjectRepository, TopicTreeBySubjectElementRepository subjectTopicTreeElementRepository,
                    TopicResourceRepository topicResourceRepository,
                    SubjectTopicRepository subjectTopicRepository, TopicSubtopicRepository topicSubtopicRepository,
                    TopicTreeSorter topicTreeSorter, SubjectService subjectService) {
        this.subjectRepository = subjectRepository;
        repository = subjectRepository;
        this.subjectTopicTreeElementRepository = subjectTopicTreeElementRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.topicTreeSorter = topicTreeSorter;
        this.subjectService = subjectService;
    }

    @GetMapping
    @ApiOperation("Gets all subjects")
    public List<SubjectIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return subjectRepository
                .findAllIncludingCachedUrlsAndTranslations()
                .stream()
                .map(subject -> new SubjectIndexDocument(subject, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single subject", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public SubjectIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return subjectRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .map(subject -> new SubjectIndexDocument(subject, language))
                .orElseThrow(() -> new NotFoundHttpRequestException("Subject not found"));
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a subject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "subject", value = "The updated subject. Fields not included will be set to null.") @RequestBody UpdateSubjectCommand command
    ) {
        doPut(id, command);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new subject")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "subject", value = "The new subject") @RequestBody CreateSubjectCommand command) {
        final var subject = new Subject();
        return doPost(subject, command);
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all topics associated with a subject", notes = "This resource is read-only. To update the relationship between subjects and topics, use the resource /subject-topics.")
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

        final var subjectTopicTree = subjectTopicTreeElementRepository.findAllBySubjectIdOrderBySubjectIdAscParentTopicIdAscTopicRankAsc(subject.getId());
        final List<Integer> topicIds;

        if (recursive) {
            topicIds = subjectTopicTree.stream()
                    .map(TopicTreeBySubjectElement::getTopicId)
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

        subjectTopics.stream()
                .filter(subjectTopic -> searchForFilterOrRelevance(subjectTopic, filterIdSet, relevanceArgument, topicSubtopics))
                .map(subjectTopic -> createSubTopicIndexDocument(subject, subjectTopic, language))
                .forEach(returnList::add);

        topicSubtopics.stream()
                .filter(topicSubtopic -> searchForFilterOrRelevance(topicSubtopic, filterIdSet, relevanceArgument, topicSubtopics))
                .map(topicSubtopic -> createSubTopicIndexDocument(subject, topicSubtopic, language))
                .forEach(returnList::add);

        // Remove duplicates from the list
        // List is sorted by parent, so we assume that any subtree that has a duplicate parent also are repeated with the same subtree
        // on the duplicates, so starting to remove duplicates should not leave any parent-less

        // (Don't know how much it makes sense to sort the list by parent and rank when duplicates are removed, but old code did)
        return topicTreeSorter.sortList(returnList).stream().distinct().collect(Collectors.toList());
    }

    private SubTopicIndexDocument createSubTopicIndexDocument(Subject subject, Object connection, String language) {
        if (connection instanceof SubjectTopic) {
            return new SubTopicIndexDocument(subject, (SubjectTopic) connection, language);
        } else if (connection instanceof TopicSubtopic) {
            return new SubTopicIndexDocument(subject, (TopicSubtopic) connection, language);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean hasFilterAndRelevanceOrJustFilterIfRelevanceIsNotSet(Topic topic, Collection<URI> filterPublicId, URI relevancePublicId) {
        final var returnValue = new AtomicBoolean(false);

        topic.getTopicFilters().forEach(topicFilter -> topicFilter.getFilter().ifPresent(filter -> {
            if (filterPublicId.contains(filter.getPublicId())) {
                if (relevancePublicId != null) {
                    topicFilter.getRelevance().ifPresent(relevance -> {
                        if (relevance.getPublicId().equals(relevancePublicId)) {
                            returnValue.set(true);
                        }
                    });
                } else {
                    returnValue.set(true);
                }
            }
        }));

        return returnValue.get();
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

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for a subject. Searches recursively in all topics belonging to this subject." +
            "The ordering of resources will be based on the rank of resources relative to the topics they belong to.")
    public List<ResourceIndexDocument> getResources(
            @PathVariable("id") URI subjectId,
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
        final var subject = subjectRepository.findFirstByPublicId(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject", subjectId));

        final var subjectTopicTree = subjectTopicTreeElementRepository.findAllBySubjectIdOrderBySubjectIdAscParentTopicIdAscTopicRankAsc(subject.getId());
        final var subjectIds = subjectTopicTree.stream()
                .map(TopicTreeBySubjectElement::getTopicId)
                .collect(Collectors.toList());

        // Populate a tree of subject->topic relations, add the resources to the list and then run sort on the list so all
        // levels are sorted on rank and relation type

        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        subjectTopicTree.forEach(treeElement -> {
            if (treeElement.getParentTopicId() == 0) {
                // This is a subjectTopic connection
                resourcesToSort.add(new TopicResourceTreeSortable("topic", "subject", treeElement.getTopicId(), treeElement.getSubjectId(), treeElement.getTopicRank()));
            } else {
                // This is a topicSubtopic connection
                resourcesToSort.add(new TopicResourceTreeSortable("topic", "topic", treeElement.getTopicId(), treeElement.getParentTopicId(), treeElement.getTopicRank()));
            }
        });

        final Set<URI> filterIdSet = filterIds != null ? Set.of(filterIds) : Set.of();
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final List<TopicResource> topicResources;
        if (filterIdSet.size() > 0 && resourceTypeIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(subjectIds, filterIdSet, resourceTypeIdSet, relevanceArgument);
        } else if (filterIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(subjectIds, filterIdSet, relevanceArgument);
        } else if (resourceTypeIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(subjectIds, resourceTypeIdSet, relevanceArgument);
        } else {
            topicResources = topicResourceRepository.findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(subjectIds, relevanceArgument);
        }

        topicResources.forEach(topicResource -> resourcesToSort.add(new TopicResourceTreeSortable(topicResource)));

        return topicTreeSorter
                .sortList(resourcesToSort)
                .stream()
                .map(TopicResourceTreeSortable::getTopicResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(topicResource -> new ResourceIndexDocument(topicResource, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters for a subject")
    public List<FilterIndexDocument> getFilters(@PathVariable("id") URI subjectId) {
        return subjectRepository.findFirstByPublicIdIncludingFilters(subjectId)
                .stream()
                .map(Subject::getFilters)
                .flatMap(Collection::stream)
                .map(FilterIndexDocument::new)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        try {
            subjectService.delete(id);
        } catch (NotFoundServiceException e) {
            throw new NotFoundHttpRequestException(e);
        }
    }
}
