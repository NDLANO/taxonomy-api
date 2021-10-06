package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/*
    This class replicates old structure previously implemented as recursive queries in database, methods
    generally just returns flat lists that replicates the old database views
 */
@Transactional(propagation = Propagation.MANDATORY)
@Service
public class RecursiveNodeTreeService {
    private final NodeConnectionRepository nodeConnectionRepository;

    public RecursiveNodeTreeService(NodeConnectionRepository nodeConnectionRepository) {
        this.nodeConnectionRepository = nodeConnectionRepository;
    }

    private void addChildIdsRecursively(Set<TreeElement> elements, Set<Integer> ids, int ttl) {
        // Method just takes the list of ids provided and add each of the children it finds to the list,
        // and then recursively runs the same method on each of the found children IDs, once for each level

        if (--ttl < 0) {
            throw new IllegalStateException("Recursion limit reached, probably an infinite loop in the structure");
        }

        final var idsThisLevel = new HashSet<Integer>();

        nodeConnectionRepository.findAllByNodeIdInIncludingTopicAndSubtopic(ids)
                .forEach(nodeConnection -> {
                    elements.add(new TreeElement(nodeConnection.getChildId(), nodeConnection.getParentId(), nodeConnection.getRank()));
                    idsThisLevel.add(nodeConnection.getChildId());
                });

        if (idsThisLevel.size() > 0) {
            addChildIdsRecursively(elements, idsThisLevel, ttl);
        }
    }

    public Set<TreeElement> getRecursiveNodes(Node node) {
        final var toReturn = new HashSet<TreeElement>();
        toReturn.add(new TreeElement(node.getId(), null, 0));

        addChildIdsRecursively(toReturn, Set.of(node.getId()), 1000);

        return toReturn;
    }

    public static class TreeElement {
        private final int id;
        private final Integer parentId;
        private final int rank;

        public TreeElement(int id, Integer parentId, int rank) {
            this.id = id;
            this.parentId = parentId;
            this.rank = rank;
        }

        public int getId() {
            return id;
        }

        public Optional<Integer> getParentId() {
            return Optional.ofNullable(parentId);
        }

        public int getRank() {
            return rank;
        }
    }
}
