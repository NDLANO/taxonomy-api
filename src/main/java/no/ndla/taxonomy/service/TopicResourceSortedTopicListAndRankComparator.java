package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.TopicResource;

import java.util.Comparator;
import java.util.List;

public class TopicResourceSortedTopicListAndRankComparator implements Comparator<TopicResource> {
    private final List<Integer> sortedTopicIdList;

    public TopicResourceSortedTopicListAndRankComparator(List<Integer> sortedTopicIdList) {
        this.sortedTopicIdList = sortedTopicIdList;
    }

    @Override
    public int compare(TopicResource topicResource1, TopicResource topicResource2) {
        final var listPos1 = sortedTopicIdList.indexOf(topicResource1.getTopic().getId());
        final var listPos2 = sortedTopicIdList.indexOf(topicResource2.getTopic().getId());

        // Order by topic-resource rank (not part of tree query) if at same topic and topic level
        if (listPos1 == listPos2) {
            return Integer.compare(topicResource1.getRank(), topicResource2.getRank());
        }

        return Integer.compare(listPos1, listPos2);
    }
}
