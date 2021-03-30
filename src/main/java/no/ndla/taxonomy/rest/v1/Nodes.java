package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.InjectMetadata;
import no.ndla.taxonomy.service.NodeService;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/nodes"})
public class Nodes extends CrudController<Topic> {
    private final TopicResourceTypeService topicResourceTypeService;
    private final TopicRepository topicRepository;
    private final NodeService nodeService;

    public Nodes(TopicRepository topicRepository,
                 TopicResourceTypeService topicResourceTypeService,
                 NodeService nodeService,
                 CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(topicRepository, cachedUrlUpdaterService);

        this.topicRepository = topicRepository;
        this.topicResourceTypeService = topicResourceTypeService;
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
}
