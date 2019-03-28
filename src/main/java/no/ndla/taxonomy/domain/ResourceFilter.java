package no.ndla.taxonomy.domain;


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

    public ResourceFilter(Resource resource, Filter filter, Relevance relevance, URI publicId) {
        this.filter = filter;
        this.resource = resource;
        this.relevance = relevance;
        this.setPublicId(publicId);
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

    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }

    public String toString() {
        return "ResourceFilter: { " + resource.getName() + " " + resource.getPublicId() + " --" + relevance.getName() + "--> " + filter.getName() + " " + filter.getPublicId() + " }";
    }
}
