package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Rankable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class RankableSortedListComparator implements Comparator<Rankable> {
    private final List<Integer> sortedTopicIdList;
    private Function<Rankable, Integer> getId;

    public RankableSortedListComparator(List<Integer> sortedTopicIdList, Function<Rankable, Integer> getIdMethod) {
        this.sortedTopicIdList = sortedTopicIdList;
        this.getId = getIdMethod;
    }

    @Override
    public int compare(Rankable topicResource1, Rankable topicResource2) {
        final var listPos1 = sortedTopicIdList.indexOf(getId.apply(topicResource1));
        final var listPos2 = sortedTopicIdList.indexOf(getId.apply(topicResource2));

        // Order by topic-resource rank (not part of tree query) if at same topic and topic level
        if (listPos1 == listPos2) {
            return Integer.compare(topicResource1.getRank(), topicResource2.getRank());
        }

        return Integer.compare(listPos1, listPos2);
    }
}
