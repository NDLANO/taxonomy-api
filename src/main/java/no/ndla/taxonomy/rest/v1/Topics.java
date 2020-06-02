package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateTopicCommand;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/topics"})
public class Topics extends PathResolvableEntityRestController<Topic> {
    private final TopicResourceTypeService topicResourceTypeService;
    private final TopicRepository topicRepository;
    private final TopicService topicService;
    private final MetadataEntityWrapperService metadataWrapperService;

    public Topics(TopicRepository topicRepository,
                  TopicResourceTypeService topicResourceTypeService,
                  TopicService topicService,
                  MetadataUpdateService metadataUpdateService,
                  MetadataEntityWrapperService metadataWrapperService,
                  CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(topicRepository, metadataUpdateService, cachedUrlUpdaterService);

        this.topicRepository = topicRepository;
        this.metadataWrapperService = metadataWrapperService;
        this.topicResourceTypeService = topicResourceTypeService;
        this.topicService = topicService;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Filter by contentUri")
            @RequestParam(value = "contentURI", required = false)
                    URI contentUriFilter,

            @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
            @RequestParam(required = false, defaultValue = "false")
                    boolean includeMetadata
    ) {

        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }

        return topicService.getTopics(language, contentUriFilter, includeMetadata);
    }


    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @Transactional
    public TopicDTO get(@PathVariable("id") URI id,
                        @ApiParam(value = "ISO-639-1 language code", example = "nb")
                        @RequestParam(value = "language", required = false, defaultValue = "")
                                String language,

                        @ApiParam(value = "Set to true to include metadata in response. Note: Will increase response time significantly on large queries, use only when necessary")
                        @RequestParam(required = false, defaultValue = "false")
                                boolean includeMetadata
    ) {
        return new TopicDTO(
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

    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    @Transactional
    public List<ResourceTypeDTO> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return topicResourceTypeService.getTopicResourceTypes(id)
                .stream()
                .filter(topicResourceType -> topicResourceType.getResourceType().isPresent())
                .map(topicResourceType -> new ResourceTypeWithConnectionDTO(topicResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this topic")
    @Transactional
    public List<FilterWithConnectionDTO> getFilters(
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
                .map(topicFilter -> new FilterWithConnectionDTO(topicFilter, language))
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
