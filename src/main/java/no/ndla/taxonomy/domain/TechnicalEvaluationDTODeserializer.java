/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import no.ndla.taxonomy.service.dtos.TechnicalEvaluationDTO;

public class TechnicalEvaluationDTODeserializer extends UpdateOrDelete.Deserializer<TechnicalEvaluationDTO> {
    private Optional<String> getComment(JsonNode node) {
        return Optional.ofNullable(node.get("comment")).map(JsonNode::textValue);
    }

    @Override
    protected TechnicalEvaluationDTO deserializeInner(JsonNode node) {
        var requiresEvaluationNode = node.get("requiresEvaluation");
        var requiresEvaluation = requiresEvaluationNode != null && requiresEvaluationNode.asBoolean();
        var comment = getComment(node);
        return new TechnicalEvaluationDTO(requiresEvaluation, comment);
    }
}
