package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Filter;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 *
 */
@ApiModel("SubjectFilterIndexDocument")
public class FilterIndexDocument {
    @JsonProperty
    @ApiModelProperty(value = "Filter id", example = "urn:filter:12")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "Filter name", example = "1T-YF")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
    public Optional<URI> contentUri;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterIndexDocument that = (FilterIndexDocument) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(contentUri, that.contentUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    public FilterIndexDocument() {
    }

    public FilterIndexDocument(Filter filter) {
        this.id = filter.getPublicId();
        this.name = filter.getName();
        this.contentUri = filter.getContentUri();
    }
}
