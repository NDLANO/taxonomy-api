package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiOperation;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.service.TopicReusedInSameSubjectCloningService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;

@RestController
@Transactional
public class TopicReusedInSameSubjectCloningController {
    private TopicReusedInSameSubjectCloningService topicReusedInSameSubjectCloningService;

    public TopicReusedInSameSubjectCloningController(TopicReusedInSameSubjectCloningService topicReusedInSameSubjectCloningService) {
        this.topicReusedInSameSubjectCloningService = topicReusedInSameSubjectCloningService;
    }
    @ApiOperation("Fixes reused topic within the same subject by cloning the topic")
    @RequestMapping(value = "/v1/topic/reusefix/{topic}", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public TopicReusedInSameSubjectCloningService.TopicCloningContext.TopicHierarchyFixReport reusefix(@PathVariable("topic") URI topicUri) throws TopicReusedInSameSubjectCloningService.TopicIsNotInConflictException {
        return topicReusedInSameSubjectCloningService.copyConflictingTopic(topicUri);
    }

    @ExceptionHandler(TopicReusedInSameSubjectCloningService.TopicIsNotInConflictException.class)
    @ResponseStatus(HttpStatus.GONE)
    public NotConflictingResponse notInConflict(TopicReusedInSameSubjectCloningService.TopicIsNotInConflictException e) {
        return new NotConflictingResponse(e.getTopic());
    }

    public static class NotConflictingResponse {
        @JsonIgnoreProperties({"subjects", "subtopics", "parentTopics", "resources", "filters", "translations", "primaryParentTopic"})
        private Topic topic;

        public NotConflictingResponse(Topic topic) {
            this.topic = topic;
        }

        public Topic getTopic() {
            return topic;
        }
    }
}
