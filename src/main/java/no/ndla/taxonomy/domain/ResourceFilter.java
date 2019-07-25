package no.ndla.taxonomy.domain;


import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class ResourceFilter extends DomainEntity {

    @ManyToOne
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    protected ResourceFilter() {
    }

    public ResourceFilter(Resource resource, Filter filter, Relevance relevance) {
        setPublicId(URI.create("urn:resource-filter:" + UUID.randomUUID()));

        this.setFilter(filter);
        this.setResource(resource);
        this.setRelevance(relevance);
    }

    public Filter getFilter() {
        return filter;
    }

    public Resource getResource() {
        return resource;
    }

    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    public void setRelevance(Relevance relevance) {
        final var previousRelevance = this.relevance;

        this.relevance = relevance;

        if (relevance != previousRelevance && previousRelevance != null) {
            previousRelevance.removeResourceFilter(this);
        }

        if (relevance != null && !relevance.getResourceFilters().contains(this)) {
            relevance.addResourceFilter(this);
        }
    }

    public void setResource(Resource resource) {
        final var previousResource = this.resource;

        this.resource = resource;

        if (previousResource != null && previousResource != resource) {
            previousResource.removeResourceFilter(this);
        }

        if (resource != null) {
            if (!resource.getResourceFilters().contains(this)) {
                resource.addResourceFilter(this);
            }
        }
    }

    public void setFilter(Filter filter) {
        final var previousFilter = this.filter;

        this.filter = filter;

        if (previousFilter != null && previousFilter != filter) {
            previousFilter.removeResourceFilter(this);
        }

        if (filter != null) {
            if (!filter.getResourceFilters().contains(this)) {
                filter.addResourceFilter(this);
            }
        }
    }

    public String toString() {
        return "ResourceFilter: { " + resource.getName() + " " + resource.getPublicId() + " --" + relevance.getName() + "--> " + filter.getName() + " " + filter.getPublicId() + " }";
    }

}
