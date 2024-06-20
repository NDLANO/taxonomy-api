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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;

public class UpdateOrDelete<T> {
    private Optional<T> value = Optional.empty();
    private boolean delete = false;

    private UpdateOrDelete() {}

    private UpdateOrDelete(T value, boolean delete) {
        this.value = Optional.ofNullable(value);
        this.delete = delete;
    }

    public static <T> UpdateOrDelete<T> Default() {
        return new UpdateOrDelete<>();
    }

    public boolean isDelete() {
        return delete;
    }

    public Optional<T> getValue() {
        return value;
    }

    public abstract static class Deserializer<T> extends JsonDeserializer<UpdateOrDelete<T>> {
        @Override
        public UpdateOrDelete<T> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            return new UpdateOrDelete<>(null, true);
        }

        protected abstract T deserializeInner(JsonNode node);

        @Override
        public UpdateOrDelete<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JacksonException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            if (node.isMissingNode()) {
                return new UpdateOrDelete<>(null, false);
            }

            if (node.isNull()) {
                return new UpdateOrDelete<>(null, true);
            }

            T innerValue = deserializeInner(node);
            return new UpdateOrDelete<>(innerValue, false);
        }
    }
}
