/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;

public class QualityEvaluationDTODeserializer extends JsonDeserializer<Optional<QualityEvaluationDTO>> {
    private Optional<String> getNote(JsonNode node) {
        var hasNote = node.has("note");
        var noteNode = node.get("note");
        if (hasNote && noteNode.isTextual()) {
            return Optional.of(node.get("note").asText());
        }
        return Optional.empty();
    }

    @Override
    public Optional<QualityEvaluationDTO> deserialize(
            JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.isNull()) {
            return Optional.of(null);
        }
        if (node.isMissingNode()) {
            return Optional.empty();
        }

        var gradeInt = node.get("grade").asInt();
        var grade = Grade.fromInt(gradeInt);
        var note = getNote(node);

        return Optional.of(new QualityEvaluationDTO(grade, note));
    }
}
