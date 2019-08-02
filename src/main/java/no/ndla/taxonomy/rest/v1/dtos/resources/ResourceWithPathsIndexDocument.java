package no.ndla.taxonomy.rest.v1.dtos.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@ApiModel("ResourceWithPathsIndexDocument")
public class ResourceWithPathsIndexDocument extends ResourceIndexDocument {

    @JsonProperty
    @ApiModelProperty(value = "All paths that lead to this resource", example = "[\"/subject:1/topic:1/resource:1\", \"/subject:2/topic:3/resource:1\"}")
    public List<String> paths;

    public ResourceWithPathsIndexDocument() {
        super();
    }

    public ResourceWithPathsIndexDocument(Resource resource) {
        super(resource);
        setPathsFromResource(resource);
    }

    public ResourceWithPathsIndexDocument(Resource resource, String languageCode) {
        super(resource, languageCode);
        setPathsFromResource(resource);
    }

    private void setPathsFromResource(Resource resource) {
        resource.getPrimaryPath()
                .ifPresent(this::setPath);

        this.paths = new ArrayList<>(resource.getAllPaths());
    }
}
