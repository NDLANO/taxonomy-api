package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.rest.BadHttpRequestException;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.commands.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateTopicCommand;
import no.ndla.taxonomy.rest.v1.dtos.topics.*;
import no.ndla.taxonomy.rest.v1.extractors.topics.ResourceQueryExtractor;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ndla.taxonomy.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@RestController
@RequestMapping(path = {"/v1/topics"})
@Transactional
public class Topics extends CrudController<Topic> {


    private JdbcTemplate jdbcTemplate;

    private TopicResourceTypeService topicResourceTypeService;

    private static final String TOPIC_TREE_BY_TOPIC_ID = getQuery("topic_tree_by_topic_id");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_topic_public_id_recursively");
    private static final Comparator<ResourceIndexDocument> RESOURCE_BY_RANK = Comparator.comparing(resourceNode -> resourceNode.rank);
    private static final Comparator<TopicNode> TOPIC_BY_RANK = Comparator.comparing(topicNode -> topicNode.rank);

    private TopicRepository topicRepository;
    private TopicSubtopicRepository topicSubtopicRepository;
    private SubjectTopicRepository subjectTopicRepository;
    private SubjectRepository subjectRepository;
    private TopicResourceRepository topicResourceRepository;

    public Topics(TopicRepository topicRepository,
                  TopicSubtopicRepository topicSubtopicRepository,
                  SubjectTopicRepository subjectTopicRepository,
                  TopicResourceTypeService topicResourceTypeService,
                  SubjectRepository subjectRepository,
                  TopicResourceRepository topicResourceRepository,
                  JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.topicRepository = topicRepository;
        this.repository = topicRepository;
        this.topicSubtopicRepository = topicSubtopicRepository;
        this.subjectTopicRepository = subjectTopicRepository;
        this.topicResourceTypeService = topicResourceTypeService;
        this.topicResourceRepository = topicResourceRepository;
        this.subjectRepository = subjectRepository;
    }

    private TopicIndexDocument createTopicIndexDocument(Topic topic, String language) {
        return new TopicIndexDocument() {{
            name = topic.getTranslation(language).map(TopicTranslation::getName).orElse(topic.getName());
            id = topic.getPublicId();
            contentUri = topic.getContentUri();
            path = topic.getPrimaryPath().orElse(null);
        }};
    }

    private TopicWithPathsIndexDocument createTopicWithPathsIndexDocument(Topic topic, String language) {
        return new TopicWithPathsIndexDocument() {{
            name = topic.getTranslation(language).map(TopicTranslation::getName).orElse(topic.getName());
            id = topic.getPublicId();
            contentUri = topic.getContentUri();
            path = topic.getPrimaryPath().orElse(null);
            paths = new ArrayList<>(topic.getAllPaths());
        }};
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return topicRepository.findAllIncludingCachedUrlsAndTranslations()
                .stream()
                .map(topic -> this.createTopicIndexDocument(topic, language))
                .collect(Collectors.toList());
    }


    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    public TopicWithPathsIndexDocument get(@PathVariable("id") URI id,
                                           @ApiParam(value = "ISO-639-1 language code", example = "nb")
                                           @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        return topicRepository.findFirstByPublicIdIncludingCachedUrlsAndTranslations(id)
                .map(topic -> this.createTopicWithPathsIndexDocument(topic, language))
                .orElseThrow(() -> new NotFoundHttpRequestException("Topic was not found"));
    }


    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "connection", value = "The new topic") @RequestBody CreateTopicCommand command) {
        final var topic = new Topic();
        final var result = doPost(topic, command);

        return result;
    }


    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic. Fields not included will be set to null.") @RequestBody UpdateTopicCommand command) {
        doPut(id, command);
    }


    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given topic")
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
                    URI relevance
    ) {

        final Map<Integer, TopicNode> nodeMap = jdbcTemplate.query(TOPIC_TREE_BY_TOPIC_ID, new Object[]{topicId.toString()}, this::buildTopicTree);

        TopicIndexDocument topicIndexDocument = get(topicId, null);

        if (filterIds == null || filterIds.length == 0) {
            filterIds = getSubjectFilters(subjectId);
        }

        List<Object> args = new ArrayList<>();
        String query;
        if (recursive) {
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY;
            args.add(topicId.toString());
            args.add(language);
            args.add(language);
            ResourceQueryExtractor extractor = new ResourceQueryExtractor();
            query = extractor.addResourceTypesToQuery(resourceTypeIds, args, query);
            query = extractor.addFiltersToQuery(filterIds, args, query);

            Map<Integer, TopicNode> resourceMap = jdbcTemplate.query(query, setQueryParameters(args.toArray()), resultSet -> {
                return populateTopicTree(extractor, relevance, topicIndexDocument, resultSet, nodeMap);
            });
            return resourceMap
                    .values()
                    .stream()
                    .filter(topicNode -> topicNode.level == 0)
                    .flatMap(subtopic ->
                            Stream.concat(
                                    subtopic.resources.stream().sorted(RESOURCE_BY_RANK), //resources on level 1
                                    subtopic.subTopics.stream().sorted(TOPIC_BY_RANK).flatMap(subtopic2 -> //level 2 is topic-subtopic
                                            Stream.concat(
                                                    subtopic2.resources.stream().sorted(RESOURCE_BY_RANK), //resources on level 2
                                                    subtopic2.subTopics.stream().sorted(TOPIC_BY_RANK).flatMap(sub3 -> //level 3 is topic-subtopic
                                                            sub3.resources.stream().sorted(RESOURCE_BY_RANK)) //resources on level 3
                                            )
                                    )
                            )
                    )
                    .collect(Collectors.toList());

        } else {
            final var topic = topicRepository.findFirstByPublicId(topicId).orElseThrow(() -> new NotFoundException("Topic", topicId));

            final List<TopicResource> topicResources;
            if (resourceTypeIds.length > 0 && filterIds.length > 0) {
                topicResources = topicResourceRepository.findAllByTopicAndFilterPublicIdsAndResourceTypePublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(topic, Set.of(filterIds), Set.of(resourceTypeIds));
            } else if (resourceTypeIds.length > 0) {
                topicResources = topicResourceRepository.findAllByTopicAndResourceTypePublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(topic, Set.of(resourceTypeIds));
            } else if (filterIds.length > 0) {
                topicResources = topicResourceRepository.findAllByTopicAndFilterPublicIdsIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(topic, Set.of(filterIds));
            } else {
                topicResources = topicResourceRepository.findAllByTopicIncludingResourceAndResourceTranslationsAndResourceTypesAndResourceTypeTranslationsAndCachedUrls(topic);
            }

            return topicResources.stream()
                    .filter((topicResource) -> {
                        // Filter result set based on relevance of resource if provided in query parameter

                        if (relevance == null || relevance.toString().equals("")) {
                            return true;
                        }

                        // Scan all resourceFilters on resource to check if the requested relevance is found
                        return topicResource.getResource().getResourceFilters()
                                .stream()
                                .anyMatch(resourceFilter -> {
                                    if (resourceFilter.getRelevance().isEmpty()) {
                                        return false;
                                    }
                                    return resourceFilter.getRelevance().get().getPublicId().equals(relevance);
                                });
                    })
                    .map(topicResource -> new ResourceIndexDocument(topicResource, language))
                    .collect(Collectors.toList());
        }

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

    private Map<Integer, TopicNode> populateTopicTree(ResourceQueryExtractor extractor, URI relevance, TopicIndexDocument topicIndexDocument, ResultSet resultSet, Map<Integer, TopicNode> nodeMap) throws SQLException {
        List<ResourceIndexDocument> resources = extractor.extractResources(relevance, topicIndexDocument, resultSet);

        resources.forEach(resourceIndexDocument -> {
            Integer topicId = resourceIndexDocument.topicNumericId;
            nodeMap.get(topicId).resources.add(resourceIndexDocument);
        });
        return nodeMap;
    }


    private Map<Integer, TopicNode> buildTopicTree(ResultSet resultSet) throws SQLException {
        Map<Integer, TopicNode> nodeMap = new HashMap<>();

        while (resultSet.next()) {
            int parentTopicId = resultSet.getInt("parent_topic_id");

            TopicNode t = new TopicNode();
            t.topicId = resultSet.getInt("topic_id");
            t.rank = resultSet.getInt("topic_rank");
            t.publicId = resultSet.getString("public_id");
            t.level = resultSet.getInt("topic_level");
            if (t.level == 0) {
                nodeMap.put(t.topicId, t);
            } else {
                nodeMap.get(parentTopicId).subTopics.add(t);
                nodeMap.put(t.topicId, t);
            }
        }
        return nodeMap;
    }


    @GetMapping("/{id}/resource-types")
    @ApiOperation(value = "Gets all resource types associated with this resource")
    public List<no.ndla.taxonomy.rest.v1.dtos.resources.ResourceTypeIndexDocument> getResourceTypes(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        try {
            return topicResourceTypeService.getTopicResourceTypes(id)
                    .stream()
                    .map(topicResourceType -> new no.ndla.taxonomy.rest.v1.dtos.resources.ResourceTypeIndexDocument() {{
                        ResourceType resourceType = topicResourceType.getResourceType();
                        id = resourceType.getPublicId();
                        ResourceType parent = resourceType.getParent().orElse(null);
                        parentId = parent != null ? parent.getPublicId() : null;
                        connectionId = topicResourceType.getPublicId();
                        ResourceTypeTranslation translation = language != null ? resourceType.getTranslation(language).orElse(null) : null;
                        name = translation != null ? translation.getName() : resourceType.getName();
                    }})
                    .collect(Collectors.toList());
        } catch (InvalidArgumentServiceException e) {
            throw new BadHttpRequestException(e.getMessage());
        } catch (NotFoundServiceException e) {
            throw new NotFoundHttpRequestException(e.getMessage());
        }
    }

    @GetMapping("/{id}/filters")
    @ApiOperation(value = "Gets all filters associated with this topic")
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
    public List<SubTopicIndexDocument> getSubTopics(
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
        if (filterIds == null || filterIds.length == 0) {
            filterIds = getSubjectFilters(subjectId);
        }

        String sql;
        List<Object> args;
        if (filterIds.length > 0) {
            return topicSubtopicRepository.findAllByTopicPublicIdAndFilterPublicIdsIncludingSubtopicAndSubtopicTranslations(id, Set.of(filterIds))
                    .stream()
                    .map(topicSubtopic -> new SubTopicIndexDocument(topicSubtopic, language))
                    .collect(Collectors.toList());
        } else {
            return topicSubtopicRepository.findAllByTopicPublicIdIncludingSubtopicAndSubtopicTranslations(id)
                    .stream()
                    .map(topicSubtopic -> new SubTopicIndexDocument(topicSubtopic, language))
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDocument> getAllConnections(@PathVariable("id") URI id) {
        final var results = new ArrayList<ConnectionIndexDocument>();

        topicSubtopicRepository
                .findAllBySubtopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(id)
                .stream()
                .map(ConnectionIndexDocument::parentConnection)
                .forEach(results::add);

        subjectTopicRepository
                .findAllByTopicPublicIdIncludingSubjectAndTopicAndCachedUrls(id)
                .stream()
                .map(ConnectionIndexDocument::new)
                .forEach(results::add);

        topicSubtopicRepository
                .findAllByTopicPublicIdIncludingTopicAndSubtopicAndCachedUrls(id)
                .stream()
                .map(ConnectionIndexDocument::subtopicConnection)
                .forEach(results::add);

        return results;
    }

    public static class TopicNode {

        int topicId;
        int rank;
        int level;
        String publicId;
        List<TopicNode> subTopics = new ArrayList<>();
        List<ResourceIndexDocument> resources = new ArrayList<>();
    }


}
