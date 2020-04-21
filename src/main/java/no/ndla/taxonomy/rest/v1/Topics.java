package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.repositories.TopicTreeByTopicElementRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateTopicCommand;
import no.ndla.taxonomy.rest.v1.dtos.topics.FilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.TopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.topics.TopicWithPathsIndexDocument;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.ConnectionIndexDTO;
import no.ndla.taxonomy.service.dtos.SubTopicIndexDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/topics"})
public class Topics extends PathResolvableEntityRestController<Topic> {
    private final TopicResourceTypeService topicResourceTypeService;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final TopicTreeByTopicElementRepository topicTreeRepository;
    private final TopicTreeSorter topicTreeSorter;
    private final TopicService topicService;
    private final MetadataEntityWrapperService metadataWrapperService;

    public Topics(TopicRepository topicRepository,
                  TopicResourceTypeService topicResourceTypeService,
                  SubjectRepository subjectRepository,
                  TopicResourceRepository topicResourceRepository,
                  TopicTreeByTopicElementRepository topicTreeRepository,
                  TopicTreeSorter topicTreeSorter,
                  TopicService topicService,
                  MetadataApiService metadataApiService,
                  MetadataEntityWrapperService metadataWrapperService) {
        super(metadataApiService);

        this.topicRepository = topicRepository;
        this.metadataWrapperService = metadataWrapperService;
        this.repository = topicRepository;
        this.topicResourceTypeService = topicResourceTypeService;
        this.topicResourceRepository = topicResourceRepository;
        this.subjectRepository = subjectRepository;
        this.topicTreeRepository = topicTreeRepository;
        this.topicTreeSorter = topicTreeSorter;
        this.topicService = topicService;
    }

    private TopicWithPathsIndexDocument createTopicWithPathsIndexDocument(MetadataWrappedEntity<Topic> wrappedTopic, String language) {
        final var topic = wrappedTopic.getEntity();

        return new TopicWithPathsIndexDocument() {{
            name = topic.getTranslation(language).map(TopicTranslation::getName).orElse(topic.getName());
            id = topic.getPublicId();
            contentUri = topic.getContentUri();
            path = topic.getPrimaryPath().orElse(null);
            paths = new ArrayList<>(topic.getAllPaths());
            metadata = wrappedTopic.getMetadata().orElse(null);
        }};
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    @Transactional
    public List<TopicIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {

        return metadataWrapperService.wrapEntities(topicRepository.findAllIncludingCachedUrlsAndTranslations(), includeMetadata)
                .stream()
                .map(topic -> this.createTopicWithPathsIndexDocument(topic, language))
                .collect(Collectors.toList());
    }


    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @Transactional
    public TopicWithPathsIndexDocument get(@PathVariable("id") URI id,
                                           @ApiParam(value = "ISO-639-1 language code", example = "nb")
                                           @RequestParam(value = "language", required = false, defaultValue = "")
                                                   String language,

                                           @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
                                               @RequestParam(required = false, defaultValue = "false")
                                                       boolean includeMetadata
    ) {
        return this.createTopicWithPathsIndexDocument(
                metadataWrapperService.wrapEntity(
                        topicRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id).orElseThrow(() -> new NotFoundHttpResponseException("Topic was not found")),
                        includeMetadata),
                language
        );
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(@ApiParam(name = "connection", value = "The new topic") @RequestBody CreateTopicCommand command) {
        return doPost(new Topic(), command);
    }


    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic. Fields not included will be set to null.") @RequestBody UpdateTopicCommand command) {
        doPut(id, command);
    }


    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given topic")
    @Transactional
    public List<ResourceIndexDocument> getResources(
            @ApiParam(value = "id", required = true)
            @PathVariable("id") URI topicId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, resources from subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "subject", required = false, defaultValue = "")
            @ApiParam(value = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    URI subjectId,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {
        final var topic = topicRepository.findFirstByPublicId(topicId).orElseThrow(() -> new NotFoundException("Topic", topicId));

        final List<TopicResource> topicResources;
        final Set<Integer> topicIdsToSearchFor;

        // Add both topics and resourceTopics to a common list that will be sorted in a tree-structure based on rank at each level
        final Set<TopicResourceTreeSortable> resourcesToSort = new HashSet<>();

        // If subject ID is specified and no filter IDs is specified, the filters are replaced with all filters directly assosiated with requested subject
        if (filterIds == null || filterIds.length == 0) {
            filterIds = getSubjectFilters(subjectId);
        }

        if (recursive) {
            final var topicList = topicTreeRepository.findAllByRootTopicIdOrTopicIdOrderByParentTopicIdAscParentTopicIdAscTopicRankAsc(topic.getId(), topic.getId());

            topicList.forEach(topicTreeElement -> resourcesToSort.add(new TopicResourceTreeSortable("topic", "topic", topicTreeElement.getTopicId(), topicTreeElement.getParentTopicId(), topicTreeElement.getTopicRank())));

            topicIdsToSearchFor = topicList.stream()
                    .map(TopicTreeByTopicElement::getTopicId)
                    .collect(Collectors.toSet());
        } else {
            topicIdsToSearchFor = Set.of(topic.getId());
        }


        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        if (resourceTypeIds.length > 0 && filterIds.length > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIdsToSearchFor, Set.of(filterIds), Set.of(resourceTypeIds), relevanceArgument);
        } else if (resourceTypeIds.length > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIdsToSearchFor, Set.of(resourceTypeIds), relevanceArgument);
        } else if (filterIds.length > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIdsToSearchFor, Set.of(filterIds), relevanceArgument);
        } else {
            topicResources = topicResourceRepository.findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingRelationsForResourceDocuments(topicIdsToSearchFor, relevanceArgument);
        }

        topicResources.forEach(topicResource -> resourcesToSort.add(new TopicResourceTreeSortable(topicResource)));

        // Sort the list, extract all the topicResource objects in between topics and return list of documents

        final var sortedList = topicTreeSorter
                .sortList(resourcesToSort)
                .stream()
                .map(TopicResourceTreeSortable::getTopicResource)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return metadataWrapperService.wrapEntities(sortedList, includeMetadata, (entity) -> entity.getResource().map(Resource::getPublicId).orElse(null)).stream()
                .map(wrappedTopicResource -> new ResourceIndexDocument(wrappedTopicResource, language))
                .collect(Collectors.toList());
    }

    private URI[] getSubjectFilters(URI subjectId) {
        if (subjectId != null) {
            return subjectRepository.findFirstByPublicIdIncludingFilters(subjectId)
                    .stream()
                    .map(Subject::getFilters)
                    .flatMap(Collection::stream)
                    .map(Filter::getPublicId)
                    .toArray(URI[]::new);
        }
        return new URI[0];
    }

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    @Transactional
    public List<no.ndla.taxonomy.rest.v1.dtos.resources.ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return topicResourceTypeService.getTopicResourceTypes(id)
                .stream()
                .filter(topicResourceType -> topicResourceType.getResourceType().isPresent())
                .map(topicResourceType -> new no.ndla.taxonomy.rest.v1.dtos.resources.ResourceTypeIndexDocument() {{
                    ResourceType resourceType = topicResourceType.getResourceType().get();
                    id = resourceType.getPublicId();
                    ResourceType parent = resourceType.getParent().orElse(null);
                    parentId = parent != null ? parent.getPublicId() : null;
                    connectionId = topicResourceType.getPublicId();
                    ResourceTypeTranslation translation = language != null ? resourceType.getTranslation(language).orElse(null) : null;
                    name = translation != null ? translation.getName() : resourceType.getName();
                }})
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this topic")
    @Transactional
    public List<FilterIndexDocument> getFilters(
            @ApiParam(value = "id", required = true)
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return topicRepository.findFirstByPublicIdIncludingFilters(id)
                .stream()
                .map(Topic::getTopicFilters)
                .flatMap(Collection::stream)
                .filter(topicFilter -> topicFilter.getFilter().isPresent())
                .map(topicFilter -> new FilterIndexDocument(topicFilter, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/topics")
    @ApiOperation(value = "Gets all subtopics for this topic")
    public List<SubTopicIndexDTO> getSubTopics(
            @ApiParam(value = "id", required = true)
            @PathVariable("id")
                    URI id,
            @RequestParam(value = "subject", required = false, defaultValue = "")
            @ApiParam(value = "Select filters by subject id if filter list is empty. Used as alternative to specify filters.")
                    URI subjectId,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all subtopics connected to this topic will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {
        if (filterIds == null) {
            filterIds = new URI[0];
        }

        if (filterIds.length == 0) {
            return topicService.getFilteredSubtopicConnections(id, subjectId, language, includeMetadata);
        }

        return topicService.getFilteredSubtopicConnections(id, Set.of(filterIds), language, includeMetadata);
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDTO> getAllConnections(@PathVariable("id") URI id) {
        return topicService.getAllConnections(id);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) {
        topicService.delete(id);
    }

}
