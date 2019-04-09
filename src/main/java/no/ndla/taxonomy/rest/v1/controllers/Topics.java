package no.ndla.taxonomy.rest.v1.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateTopicCommand;
import no.ndla.taxonomy.rest.v1.controllers.CrudController;
import no.ndla.taxonomy.rest.v1.dtos.topics.*;
import no.ndla.taxonomy.rest.v1.extractors.subjects.FilterExtractor;
import no.ndla.taxonomy.rest.v1.extractors.topics.*;
import no.ndla.taxonomy.services.PublicIdGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;

@RestController
@RequestMapping(path = {"/v1/topics"})
@Transactional
public class Topics extends CrudController<Topic> {


    private JdbcTemplate jdbcTemplate;

    private static final String TOPIC_TREE_BY_TOPIC_ID = getQuery("topic_tree_by_topic_id");
    private static final String GET_TOPICS_QUERY = getQuery("get_topics");
    private static final String GET_TOPICS_WITH_ALL_PATHS_QUERY = getQuery("get_topics_with_all_paths");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_topic_public_id_recursively");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY = getQuery("get_resources_by_topic_public_id");
    private static final String GET_FILTERS_BY_TOPIC_ID_QUERY = getQuery("get_filters_by_topic_public_id");
    private static final String GET_FILTERS_BY_SUBJECT_PUBLIC_ID_QUERY = getQuery("get_filters_by_subject_public_id");
    private static final String GET_SUBTOPICS_BY_TOPIC_ID_QUERY = getQuery("get_subtopics_by_topic_id_query");
    private static final String GET_SUBTOPICS_BY_TOPIC_ID_AND_FILTERS_QUERY = getQuery("get_subtopics_by_topic_id_and_filters_query");
    private static final String GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subject_connections_by_topic_id");
    private static final String GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subtopic_connections_by_topic_id");
    private static final String GET_PARENT_TOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_parent_topic_connections_by_topic_id");
    private static final Comparator<ResourceIndexDocument> RESOURCE_BY_RANK = Comparator.comparing(resourceNode -> resourceNode.rank);
    private static final Comparator<TopicNode> TOPIC_BY_RANK = Comparator.comparing(topicNode -> topicNode.rank);

    public Topics(TopicRepository topicRepository, JdbcTemplate jdbcTemplate, PublicIdGeneratorService publicIdGeneratorService) {
        this.jdbcTemplate = jdbcTemplate;
        repository = topicRepository;
        this.publicIdGeneratorService = publicIdGeneratorService;
    }


    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) throws Exception {
        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return jdbcTemplate.query(GET_TOPICS_QUERY, setQueryParameters(language), extractor::extractTopics
        );
    }


    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    public TopicWithPathsIndexDocument get(@PathVariable("id") URI id,
                                           @ApiParam(value = "ISO-639-1 language code", example = "nb")
                                           @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        TopicWithAllPathsQueryExtractor extractor = new TopicWithAllPathsQueryExtractor();
        return jdbcTemplate.query(GET_TOPICS_WITH_ALL_PATHS_QUERY, setQueryParameters(language, id.toString()), extractor::extractTopic);
    }


    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "connection", value = "The new topic") @RequestBody CreateTopicCommand command) {
        if(command.id == null){
            command.id = publicIdGeneratorService.getNext("urn:topic:");
        }
        return doPost(new Topic(), command);
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
    ) throws Exception {

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
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY;
            args.add(language);
            args.add(language);
            args.add(topicId.toString());
            ResourceQueryExtractor extractor = new ResourceQueryExtractor();
            query = extractor.addResourceTypesToQuery(resourceTypeIds, args, query);
            query = extractor.addFiltersToQuery(filterIds, args, query);

            return jdbcTemplate.query(query, setQueryParameters(args.toArray()), resultSet -> {
                return extractor.extractResources(relevance, topicIndexDocument, resultSet);
            });
        }

    }

    private URI[] getSubjectFilters(URI subjectId) {
        if (subjectId != null) {
            FilterExtractor extractor = new FilterExtractor();
            return jdbcTemplate
                    .query(GET_FILTERS_BY_SUBJECT_PUBLIC_ID_QUERY, setQueryParameters(subjectId.toString()), extractor::extractFilters)
                    .stream().map(filter -> filter.id).toArray(URI[]::new);
        }
        return new URI[]{};
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
        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(GET_FILTERS_BY_TOPIC_ID_QUERY, setQueryParameters(id.toString()),
                extractor::extractFilters
        );
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
            sql = GET_SUBTOPICS_BY_TOPIC_ID_AND_FILTERS_QUERY;
            StringBuffer filtersCombined = new StringBuffer();
            Arrays.stream(filterIds).forEach(filtersCombined::append);
            args = Arrays.asList(language, id.toString(), filtersCombined.toString());
        } else {
            sql = GET_SUBTOPICS_BY_TOPIC_ID_QUERY.replace("1 = 1", "t.public_id = ?");
            args = Arrays.asList(language, id.toString());
        }
        SubTopicQueryExtractor extractor = new SubTopicQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args.toArray()),
                extractor::extractSubTopics
        );
    }

    @GetMapping("/{id}/connections")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDocument> getAllConnections(@PathVariable("id") URI id) {
        List<ConnectionIndexDocument> results = new ArrayList<>();
        ConnectionQueryExtractor ConnectionQueryExtractor = new ConnectionQueryExtractor();
        results.addAll(jdbcTemplate.query(GET_PARENT_TOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(id.toString()), ConnectionQueryExtractor::extractConnections));
        results.addAll(jdbcTemplate.query(GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(id.toString()), ConnectionQueryExtractor::extractConnections));
        results.addAll(jdbcTemplate.query(GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(id.toString()), ConnectionQueryExtractor::extractConnections));
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
