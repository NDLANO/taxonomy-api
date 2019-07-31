package no.ndla.taxonomy.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

@Component
public class TopicTreeSorter {
    private <T extends Sortable> List<T> addElements(Map<URI, Collection<T>> elementsToAddFrom, URI parentId) {
        ArrayList<T> itemsToAdd = new ArrayList<>();

        elementsToAddFrom.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparingInt(Sortable::getSortableRank))
                .forEachOrdered(element -> {
                    itemsToAdd.add(element);
                    itemsToAdd.addAll(addElements(elementsToAddFrom, element.getSortableId()));
                });

        return itemsToAdd;
    }

    public <T extends Sortable> List<T> sortList(Collection<T> elements) {
        final var elementsByParent = new HashMap<URI, Collection<T>>();

        for (var element : elements) {
            var foundParent = false;
            if (element.getSortableParentId() != null) {
                for (var element2 : elements) {
                    if (element2.getSortableId().equals(element.getSortableParentId())) {
                        foundParent = true;
                    }
                }
            }

            final URI parentId;

            if (!foundParent) {
                parentId = null;
            } else {
                parentId = element.getSortableParentId();
            }

            elementsByParent.computeIfAbsent(parentId, k -> new ArrayList<>());
            elementsByParent.get(parentId).add(element);
        }

        return new ArrayList<>(this.addElements(elementsByParent, null));
    }

    public interface Sortable {
        @JsonIgnore
        int getSortableRank();

        @JsonIgnore
        URI getSortableId();

        @JsonIgnore
        URI getSortableParentId();
    }
}
