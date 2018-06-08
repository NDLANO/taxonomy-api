package no.ndla.taxonomy.rest.v1;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/contexts"})
@Transactional
public class Contexts {
    private static final String GET_CONTEXTS_QUERY = getQuery("get_contexts");

    private JdbcTemplate jdbcTemplate;
    private TopicRepository topicRepository;

    public Contexts(JdbcTemplate jdbcTemplate, TopicRepository topicRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.topicRepository = topicRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READONLY')")
    public List<ContextIndexDocument> get(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return jdbcTemplate.query(GET_CONTEXTS_QUERY, setQueryParameters(asList(language, language)),
                (resultSet, rowNum) -> new ContextIndexDocument() {{
                    name = resultSet.getString("context_name");
                    id = getURI(resultSet, "context_public_id");
                    path = resultSet.getString("context_path");
                }}
        );
    }

    @PostMapping
    @ApiOperation(value = "Adds a new context", notes="All subjects are already contexts and may not be added again. Only topics may be added as a context. The topic must exist already.")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "context", value = "the new context") @RequestBody CreateContextCommand command) throws Exception {
        Topic topic = topicRepository.getByPublicId(command.id);
        topic.setContext(true);
        URI location = URI.create("/v1/contexts/" + topic.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Removes a context", notes = "Does not remove the underlying resource, only marks it as not being a context")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        Topic topic = topicRepository.getByPublicId(id);
        topic.setContext(false);
    }

    public static class ContextIndexDocument {
        public URI id;
        public String path;
        public String name;
    }

    public static class CreateContextCommand {
        public URI id;
    }
}
