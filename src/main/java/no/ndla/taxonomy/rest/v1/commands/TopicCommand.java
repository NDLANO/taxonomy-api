/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

public class TopicCommand implements UpdatableDto<Node> {
    @JsonProperty
    @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:topic:1")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
    public String name;

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public void apply(Node topic) {
        if (getId().isPresent())
            topic.setPublicId(id);
        topic.setName(name);
        topic.setContentUri(contentUri);
    }
}
