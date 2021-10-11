/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import no.ndla.taxonomy.domain.Topic;

@ApiModel("Topic")
public class TopicDTO extends EntityWithPathDTO {
    public TopicDTO() {}

    public TopicDTO(Topic topic, String languageCode) {
        super(topic, languageCode);
    }
}
