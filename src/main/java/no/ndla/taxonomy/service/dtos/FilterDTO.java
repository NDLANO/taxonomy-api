package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.FilterTranslation;

import java.net.URI;

@ApiModel("Filter")
public class FilterDTO {
    @JsonProperty
    @ApiModelProperty(example = "urn:filter:1")
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
    public URI contentUri;

    public FilterDTO() {

    }

    public FilterDTO(Filter filter, String languageCode) {
        this.id = filter.getPublicId();

        this.name = filter
                .getTranslation(languageCode)
                .map(FilterTranslation::getName)
                .orElse(filter.getName());

        filter.getContentUri().ifPresent(contentUri -> this.contentUri = contentUri);
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getContentUri() {
        return contentUri;
    }
}
