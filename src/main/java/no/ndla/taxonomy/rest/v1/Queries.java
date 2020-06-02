package no.ndla.taxonomy.rest.v1;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.TopicDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = {"/v1/queries"})
public class Queries {
    private final Topics topicController;
    private final Resources resourceController;

    public Queries(Topics topicController, Resources resourceController) {
        this.topicController = topicController;
        this.resourceController = resourceController;
    }

    @GetMapping("/resources")
    @ApiOperation(value = "Gets a list of resources matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/resources?contentURI= instead")
    public List<ResourceDTO> queryResources(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return resourceController.index(language, contentURI, false);
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found. DEPRECATED: Use /v1/topics?contentURI= instead")
    public List<TopicDTO> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return topicController.index(language, contentURI, false);
    }
}
