package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.FilterTranslation;
import no.ndla.taxonomy.service.MetadataWrappedEntity;
import no.ndla.taxonomy.service.UpdatableDto;

import java.net.URI;
import java.util.Optional;

@ApiModel("Filter")
public class FilterDTO implements UpdatableDto<Filter> {
    @JsonProperty
    @ApiModelProperty(example = "urn:filter:1")
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "This subject the filter is connected to")
    public URI subjectId;

    @JsonProperty
    @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
    public URI contentUri;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public FilterDTO() {

    }

    public FilterDTO(Filter filter, String languageCode) {
        this.id = filter.getPublicId();

        this.name = filter
                .getTranslation(languageCode)
                .map(FilterTranslation::getName)
                .orElse(filter.getName());

        filter.getContentUri().ifPresent(contentUri -> this.contentUri = contentUri);
        filter.getSubject().ifPresent(subject -> this.subjectId = subject.getPublicId());
    }

    public FilterDTO(MetadataWrappedEntity<Filter> wrappedFilter, String languageCode) {
        this(wrappedFilter.getEntity(), languageCode);

        wrappedFilter.getMetadata().ifPresent(metadata -> this.metadata = metadata);
    }

    public String getName() {
        return name;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public URI getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(URI subjectId) {
        this.subjectId = subjectId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    @Override
    public Optional<URI> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(URI id) {
        this.id = id;
    }

    @Override
    public void apply(Filter filter) {
        filter.setName(name);
        filter.setContentUri(contentUri);
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    protected void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }
}
