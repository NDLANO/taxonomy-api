package no.ndla.taxonomy.service.domain;


import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
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
        this.filter = filter;
        this.resource = resource;
        this.relevance = relevance;
        setPublicId(URI.create("urn:filter-resource:" + UUID.randomUUID()));
    }

    public Filter getFilter() {
        return filter;
    }

    public Resource getResource() {
        return resource;
    }

    public Relevance getRelevance() {
        return relevance;
    }

    public String toString() {
        return "ResourceFilter: { " + resource.getName() + " " + resource.getPublicId() + " --" + relevance.getName() + "--> " + filter.getName() + " " + filter.getPublicId() + " }";
    }
}
