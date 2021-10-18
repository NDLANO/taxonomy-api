/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;

import java.util.Optional;
import java.util.Set;

public interface RecursiveTopicTreeService {
    Set<TopicTreeElement> getRecursiveTopics(Topic topic);

    Set<TopicTreeElement> getRecursiveTopics(Subject subject);

    class TopicTreeElement {
        private final int topicId;
        private final Integer parentSubjectId;
        private final Integer parentTopicId;
        private final int rank;

        public TopicTreeElement(int topicId, Integer parentSubjectId, Integer parentTopicId, int rank) {
            this.topicId = topicId;
            this.parentSubjectId = parentSubjectId;
            this.parentTopicId = parentTopicId;
            this.rank = rank;
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
