package no.ndla.taxonomy.domain;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class ParentNotFoundException extends RuntimeException {
    public ParentNotFoundException(Topic topic, Subject subject) {
        super("Topic with id " + topic.getPublicId() + " has no parent subject with id " + subject.getPublicId());
    }

    public ParentNotFoundException(Topic subtopic, Topic parent) {
        super("Topic with id " + subtopic.getPublicId() + " has no parent topic with id " + parent.getPublicId());
    }

    public ParentNotFoundException(Resource resource, Topic topic) {
        super("Resource with id " + resource.getPublicId() + " has no parent topic with id " + topic.getPublicId());
    }
}
