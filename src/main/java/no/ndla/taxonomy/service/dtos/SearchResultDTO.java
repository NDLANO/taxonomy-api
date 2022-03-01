package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel("SearchResult")
public class SearchResultDTO<T> {
    @JsonProperty
    @ApiModelProperty(example = "Total search result count, useful for fetching multiple pages")
    private long totalCount;

    @JsonProperty
    @ApiModelProperty(example = "The page number")
    private int page;

    @JsonProperty
    @ApiModelProperty(example = "The page size")
    private int pageSize;

    @JsonProperty
    @ApiModelProperty(example = "List of search results")
    private List<T> results;

    public SearchResultDTO(long totalCount, int pageNumber, int pageSize, List<T> results) {
        this.totalCount = totalCount;
        this.page = pageNumber;
        this.pageSize = pageSize;
        this.results = results;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<T> getResults() {
        return results;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
