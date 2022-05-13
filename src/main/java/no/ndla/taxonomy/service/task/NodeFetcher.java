/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.CustomField;
import no.ndla.taxonomy.domain.CustomFieldValue;
import no.ndla.taxonomy.domain.Metadata;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.repositories.CustomFieldRepository;
import no.ndla.taxonomy.repositories.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NodeFetcher extends VersionSchemaFetcher<Node> {

    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    CustomFieldRepository customFieldRepository;

    @Override
    protected Optional<Node> callInternal() {
        Optional<Node> node = nodeRepository.fetchNodeGraphByPublicId(this.publicId);
        if (addCustomField && node.isPresent()) {
            if (node.get().getMetadata().getCustomFieldValues().stream()
                    .map(customFieldValue -> customFieldValue.getCustomField().getKey()).collect(Collectors.toList())
                    .contains(CustomField.IS_PUBLISHING)) {
                return node;
            }
            Optional<CustomField> customField = customFieldRepository.findByKey(CustomField.IS_PUBLISHING);
            if (customField.isEmpty()) {
                customField = Optional.of(customFieldRepository.save(new CustomField(CustomField.IS_PUBLISHING)));
            }
            Metadata metadata = node.get().getMetadata();
            metadata.addCustomFieldValue(new CustomFieldValue(customField.get(), "true"));
            Node updated = nodeRepository.save(node.get());
            return Optional.of(updated);
        }
        return node;
    }
}
