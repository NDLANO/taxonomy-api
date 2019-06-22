package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.commands.CreateSubjectCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateSubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.FilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.rest.v1.extractors.subjects.ResourceExctractor;
import no.ndla.taxonomy.rest.v1.extractors.subjects.TopicExtractor;
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

import static no.ndla.taxonomy.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@RestController
@Transactional
@RequestMapping(path = {"/v1/subjects"})
public class Subjects extends CrudController<Subject> {
    private static final String RESOURCES_BY_SUBJECT_ID = getQuery("resources_by_subject_id");
    private static final String TOPIC_TREE_BY_SUBJECT_ID = getQuery("topic_tree_by_subject_id");

    private static final String GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_topics_by_subject_public_id_recursively");

    private SubjectRepository subjectRepository;
    private JdbcTemplate jdbcTemplate;
    private static final Comparator<ResourceIndexDocument> RESOURCE_BY_RANK = Comparator.comparing(resourceNode -> resourceNode.rank);
    private static final Comparator<TopicNode> TOPIC_BY_RANK = Comparator.comparing(topicNode -> topicNode.rank);

    public Subjects(SubjectRepository subjectRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.jdbcTemplate = jdbcTemplate;
        repository = subjectRepository;
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
        final var subject = doPut(id, command);
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
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) {
        String sql = GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY;
        if (!recursive) sql = sql.replace("1 = 1", "t.level = 0");

        if (filterIds == null || filterIds.length == 0) {
            filterIds = getFilters(id).stream().map(filterIndexDocument -> filterIndexDocument.id).toArray(URI[]::new);
        }

        TopicExtractor extractor = new TopicExtractor();
        URI[] finalFilterIds = filterIds;
        List<SubTopicIndexDocument> results = jdbcTemplate.query(sql, setQueryParameters(id.toString(), language), resultSet -> {
            return extractor.extractTopics(id, finalFilterIds, relevance, resultSet);
        });

        if (results == null) {
            return List.of();
        }

        if (!recursive) {
            results.sort(Comparator.comparing(o -> o.rank));
        } else {
            //sort input by path length so parent nodes are processed first (then we can add to them)
            results.sort(Comparator.comparing(o -> o.path.length()));

            //temp structures for creating a sorted tree
            ArrayList<SubTopicIndexDocument> levelOneItems = new ArrayList<>();
            Map<String, List<SubTopicIndexDocument>> mappedChildren = new HashMap<>();

            results.forEach(
                    subTopicIndexDocument -> {
                        String idInPathFormat = id.toString().substring(4);
                        String pathWithoutSubject = subTopicIndexDocument.path.replace("/" + idInPathFormat + "/", "");
                        String[] pathElements = pathWithoutSubject.split("/");
                        if (pathElements.length == 1) {
                            levelOneItems.add(subTopicIndexDocument);
                        } else {
                            int parentIndex = pathElements.length - 2;
                            mappedChildren.get(pathElements[parentIndex]).add(subTopicIndexDocument);
                        }
                        mappedChildren.putIfAbsent(subTopicIndexDocument.id.toString().substring(4), subTopicIndexDocument.children);
                    }
            );
            //sort all child lists members by their rank relative to the parent
            mappedChildren.values().forEach(childList -> childList.sort(Comparator.comparing(child -> Integer.valueOf(child.rank))));
            //sort the top level list
            levelOneItems.sort(Comparator.comparing(o -> o.rank));
            //flatten tree with (potentially) 3 levels to one
            return levelOneItems.stream()
                    .flatMap(levelOneItem ->
                            Stream.concat(Stream.of(levelOneItem), levelOneItem.children.stream()
                                    .flatMap(levelTwoItem ->
                                            Stream.concat(Stream.of(levelTwoItem), levelTwoItem.children.stream()))))
                    .collect(Collectors.toList());
        }
        return results;
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
        final Map<Integer, TopicNode> nodeMap = jdbcTemplate.query(TOPIC_TREE_BY_SUBJECT_ID, new Object[]{subjectId.toString()}, this::buildTopicTree);

        List<Object> args = new ArrayList<>();
        args.add(subjectId.toString());
        args.add(language); //resource
        args.add(language); //resource type
        String resourceQuery = addResourceTypesToQuery(resourceTypeIds, RESOURCES_BY_SUBJECT_ID, args);
        if (filterIds == null || filterIds.length == 0) {
            filterIds = getFilters(subjectId).stream().map(filterIndexDocument -> filterIndexDocument.id).toArray(URI[]::new);
        }
        resourceQuery = addFiltersToQuery(filterIds, resourceQuery, args);

        Map<Integer, TopicNode> resourceMap = jdbcTemplate.query(resourceQuery, args.toArray(), resultSet -> {
            return populateTopicTree(subjectId, relevance, resultSet, nodeMap);
        });

        //turn tree of topics and resources into a list of only resources, sorted by their rank relative to parent topic in the tree
        return resourceMap.values().stream()
                .filter(topicNode -> topicNode.level == 0).sorted(TOPIC_BY_RANK) //level 1 is subject-topics
                .flatMap(subjectTopic ->
                        Stream.concat(
                                subjectTopic.resources.stream().sorted(RESOURCE_BY_RANK), //resources on level 1
                                subjectTopic.subTopics.stream().sorted(TOPIC_BY_RANK).flatMap(subtopics1 -> //level 2 is topic-subtopic
                                        Stream.concat(
                                                subtopics1.resources.stream().sorted(RESOURCE_BY_RANK), //resources on level 2
                                                subtopics1.subTopics.stream().sorted(TOPIC_BY_RANK).flatMap(sub2 -> //level 3 is topic-subtopic
                                                        sub2.resources.stream().sorted(RESOURCE_BY_RANK)) //resources on level 3
                                        )
                                )
                        )
                ).collect(Collectors.toList());
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

    private Map<Integer, TopicNode> populateTopicTree(URI subjectURI, URI relevance, ResultSet resourceResults, Map<Integer, TopicNode> nodeMap) throws SQLException {
        ResourceExctractor extractor = new ResourceExctractor();

        List<ResourceIndexDocument> resourceIndexDocuments = extractor.extractResources(subjectURI, relevance, resourceResults);
        resourceIndexDocuments.forEach(resourceIndexDocument -> {
            Integer topicId = resourceIndexDocument.topicNumericId;
            nodeMap.get(topicId).resources.add(resourceIndexDocument);
        });

        return nodeMap;
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

    private String addFiltersToQuery(URI[] filterIds, String sql, List<Object> args) {
        if (filterIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI filterId : filterIds) {
                where.append("f.public_id = ? OR ");
                args.add(filterId.toString());
            }
            where.setLength(where.length() - 4); //remove the last " OR "
            sql = sql.replace("2 = 2", "(" + where + ")");
        }
        return sql;
    }

    private String addResourceTypesToQuery(URI[] resourceTypeIds, String sql, List<Object> args) {
        if (resourceTypeIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI resourceTypeId : resourceTypeIds) {
                where.append("rt.public_id = ? OR ");
                args.add(resourceTypeId.toString());
            }
            where.setLength(where.length() - " OR ".length());
            sql = sql.replace("1 = 1", "(" + where + ") ");
        }
        return sql;
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
