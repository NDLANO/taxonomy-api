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
    @Override
    protected TechnicalEvaluationDTO deserializeInner(JsonNode node) {
        var requiresEvaluation = Optional.ofNullable(node.get("requiresEvaluation"))
                .filter(JsonNode::isBoolean)
                .map(JsonNode::booleanValue);
        var comment = Optional.ofNullable(node.get("comment")).map(JsonNode::textValue);
        return new TechnicalEvaluationDTO(requiresEvaluation, comment);
    }
}
