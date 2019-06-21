package no.ndla.taxonomy.rest.v1;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.v1.dtos.queries.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.queries.TopicIndexDocument;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/queries"})
public class Queries {
    private ResourceRepository resourceRepository;
    private TopicRepository topicRepository;

    public Queries(ResourceRepository resourceRepository, TopicRepository topicRepository) {
        this.resourceRepository = resourceRepository;
        this.topicRepository = topicRepository;
    }

    @GetMapping("/resources")
    @ApiOperation(value = "Gets a list of resources matching given contentURI, empty list of no matches are found.")
    public List<ResourceIndexDocument> queryResources(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return resourceRepository
                .findAllByContentUriIncludingCachedUrlsAndResourceTypesAndTranslations(contentURI)
                .stream()
                .map(resource -> new ResourceIndexDocument(resource, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/topics")
    @ApiOperation(value = "Gets a list of topics matching given contentURI, empty list of no matches are found.")
    public List<TopicIndexDocument> queryTopics(
            @RequestParam("contentURI") URI contentURI,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return topicRepository
                .findAllByContentUriIncludingCachedUrlsAndTranslations(contentURI)
                .stream()
                .map(topic -> new TopicIndexDocument(topic, language))
                .collect(Collectors.toList());
    }
}
