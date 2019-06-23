package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NotFoundException;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTopicTreeElement;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.SubjectTopicTreeElementRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.rest.v1.commands.CreateSubjectCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateSubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.FilterIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.ResourceIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.rest.v1.extractors.subjects.TopicExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ndla.taxonomy.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.jdbc.QueryUtils.setQueryParameters;

@RestController
@Transactional
@RequestMapping(path = {"/v1/subjects"})
public class Subjects extends CrudController<Subject> {
    private static final String GET_TOPICS_BY_SUBJECT_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_topics_by_subject_public_id_recursively");

    private SubjectRepository subjectRepository;
    private SubjectTopicTreeElementRepository subjectTopicTreeElementRepository;
    private TopicResourceRepository topicResourceRepository;
    private JdbcTemplate jdbcTemplate;

    public Subjects(SubjectRepository subjectRepository, SubjectTopicTreeElementRepository subjectTopicTreeElementRepository,
                    TopicResourceRepository topicResourceRepository, JdbcTemplate jdbcTemplate) {
        this.subjectRepository = subjectRepository;
        this.jdbcTemplate = jdbcTemplate;
        repository = subjectRepository;
        this.subjectTopicTreeElementRepository = subjectTopicTreeElementRepository;
        this.topicResourceRepository = topicResourceRepository;
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
        final var subject = subjectRepository.findFirstByPublicId(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject", subjectId));

        final var subjectTopicTree = subjectTopicTreeElementRepository.findAllBySubjectIdOrderBySubjectIdAscParentTopicIdAscTopicRankAsc(subject.getId());
        final var subjectIds = subjectTopicTree.stream()
                .map(SubjectTopicTreeElement::getTopicId)
                .collect(Collectors.toList());
        final var topicOrderedList = subjectTopicTree.stream()
                .filter(subjectTopicTreeElement -> subjectTopicTreeElement.getTopicId() > 0)
                .map(SubjectTopicTreeElement::getTopicId)
                .collect(Collectors.toList());

        final Set<URI> filterIdSet = filterIds != null ? Set.of(filterIds) : Set.of();
        final Set<URI> resourceTypeIdSet = resourceTypeIds != null ? Set.of(resourceTypeIds) : Set.of();

        // If null is sent to query it will be ignored, otherwise it will filter by relevance
        final var relevanceArgument = relevance == null || relevance.toString().equals("") ? null : relevance;

        final List<TopicResource> topicResources;
        if (filterIdSet.size() > 0 && resourceTypeIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(subjectIds, filterIdSet, resourceTypeIdSet, relevanceArgument);
        } else if (filterIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceFilterFilterPublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(subjectIds, filterIdSet, relevanceArgument);
        } else if (resourceTypeIdSet.size() > 0) {
            topicResources = topicResourceRepository.findAllByTopicIdsAndResourceTypePublicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(subjectIds, resourceTypeIdSet, relevanceArgument);
        } else {
            topicResources = topicResourceRepository.findAllByTopicIdsAndRelevancePublicIdIfNotNullIncludingResourceAndResourceTranslationsAndCachedUrlsAndResourceResourceTypes(subjectIds, relevanceArgument);
        }

        return topicResources.stream()
                .sorted((topicResource1, topicResource2) -> {
                    // Re-order the resources in the same order as the subjectTopicTreeElement query result
                    final var listPos1 = topicOrderedList.indexOf(topicResource1.getTopic().getId());
                    final var listPos2 = topicOrderedList.indexOf(topicResource2.getTopic().getId());

                    // Order by topic-resource rank (not part of tree query) if at same topic and topic level
                    if (listPos1 == listPos2) {
                        return Integer.compare(topicResource1.getRank(), topicResource2.getRank());
                    }

                    return Integer.compare(listPos1, listPos2);
                })
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
}
