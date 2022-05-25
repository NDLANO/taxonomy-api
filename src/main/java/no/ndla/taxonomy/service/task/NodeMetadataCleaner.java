/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.CustomField;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NodeMetadataCleaner extends VersionSchemaFetcher<Node> {

    @Autowired
    NodeRepository nodeRepository;

    @Override
    protected Optional<Node> callInternal() {
        Optional<Node> maybeNode = nodeRepository.findFirstByPublicId(this.publicId);
        if (maybeNode.isPresent()) {
            Node node = maybeNode.get();
            var isPublishing = node
                    .getMetadata().getCustomFieldValues().stream().filter(customFieldValue -> customFieldValue
                            .getCustomField().getKey().equals(CustomField.IS_PUBLISHING))
                    .collect(Collectors.toList()).stream().findFirst();
            isPublishing.ifPresent(customFieldValue -> node.getMetadata().removeCustomFieldValue(customFieldValue));
            var requestPublish = node
                    .getMetadata().getCustomFieldValues().stream().filter(customFieldValue -> customFieldValue
                            .getCustomField().getKey().equals(CustomField.REQUEST_PUBLISH))
                    .collect(Collectors.toList()).stream().findFirst();
            requestPublish.ifPresent(customFieldValue -> node.getMetadata().removeCustomFieldValue(customFieldValue));
            nodeRepository.save(node);
            return Optional.of(node);
        }
        return Optional.empty();
    }
}
