package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public interface RecursiveTopicTreeService {
    /**
     *
     * @param topic The root
     * @return
     */
    Set<TopicTreeElement> getRecursiveTopics(Topic topic);

    class TopicTreeElement {
        private final int topicId;
        private final Integer parentSubjectId;
        private final Integer parentTopicId;
        private final int rank;

        public TopicTreeElement(int topicId, Integer parentTopicId, URI parentPublicId, int rank) {
            this.topicId = topicId;
            if (parentPublicId != null && parentPublicId.toString().startsWith("urn:subject:")) {
                this.parentSubjectId = parentTopicId;
                this.parentTopicId = null;
            } else {
                this.parentSubjectId = null;
                this.parentTopicId = parentTopicId;
            }
            this.rank = rank;
        }
        public TopicTreeElement(int topicId, Topic parentTopic, int rank) {
            this(topicId, parentTopic.getId(), parentTopic.getPublicId(), rank);
        }

        public int getTopicId() {
            return topicId;
        }

        public Optional<Integer> getParentSubjectId() {
            return Optional.ofNullable(parentSubjectId);
        }

        public Optional<Integer> getParentTopicId() {
            return Optional.ofNullable(parentTopicId);
        }

        public int getRank() {
            return rank;
        }
    }
}
