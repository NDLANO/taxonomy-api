package no.ndla.taxonomy.rest.v1.dtos.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class NodeResourcePageDTO {
    @JsonProperty
    @Schema(description = "Total number of elements")
    public long totalCount;

    @JsonProperty
    @Schema(description = "Page containing results")
    public List<NodeResourceDTO> results;

    public NodeResourcePageDTO() {
    }

    public NodeResourcePageDTO(long totalCount, List<NodeResourceDTO> results) {
        this.totalCount = totalCount;
        this.results = results;
    }
}
