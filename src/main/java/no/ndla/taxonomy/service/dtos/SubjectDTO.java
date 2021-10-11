/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectTranslation;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.service.MetadataIdField;

import java.net.URI;

/**
 *
 */
@ApiModel("Subject")
public class SubjectDTO extends EntityWithPathDTO {
    public SubjectDTO() {}

    public SubjectDTO(Subject subject, String languageCode) {
        super(subject, languageCode);
    }
}
