package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/nodes"})
public class Nodes extends CrudController<Topic> {
    private final NodeResourceTypeService nodeResourceTypeService;
    private final TopicRepository topicRepository;
    private final NodeService nodeService;

    public Nodes(TopicRepository topicRepository,
                 NodeResourceTypeService nodeResourceTypeService,
                 NodeService nodeService,
                 CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(topicRepository, cachedUrlUpdaterService);

        this.topicRepository = topicRepository;
        this.nodeResourceTypeService = nodeResourceTypeService;
        this.nodeService = nodeService;
    }

    @GetMapping
    @ApiOperation("Gets all nodes")
    public List<NodeDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,

            @ApiParam(value = "Filter by contentUri")
            @RequestParam(value = "contentURI", required = false)
                    URI contentUriFilter
    ) {

        if (contentUriFilter != null && contentUriFilter.toString().equals("")) {
            contentUriFilter = null;
        }

        return nodeService.getNodes(language, contentUriFilter);
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single node")
    @Transactional
    @InjectMetadata
    public NodeDTO get(@PathVariable("id") URI id,
                        @ApiParam(value = "ISO-639-1 language code", example = "nb")
                        @RequestParam(value = "language", required = false, defaultValue = "")
                                String language
    ) {
        return new NodeDTO(topicRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id).orElseThrow(() -> new NotFoundHttpResponseException("Node was not found")), language);
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
        return nodeResourceTypeService.getNodeResourceTypes(id)
                .stream()
                .filter(topicResourceType -> topicResourceType.getResourceType().isPresent())
                .map(topicResourceType -> new ResourceTypeWithConnectionDTO(topicResourceType, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/nodes")
    @ApiOperation(value = "Gets all subnodes for this topic")
    public List<SubNodeIndexDTO> getSubTopics(
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
                    String language
    ) {
        if (filterIds == null) {
            filterIds = new URI[0];
        }

        if (filterIds.length == 0) {
            return nodeService.getSubnodeConnections(id, subjectId, language);
        }

        return List.of(); // Requested filtered, we don't have filters.
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDTO> getAllConnections(@PathVariable("id") URI id) {
        return nodeService.getAllConnections(id);
    }
}
