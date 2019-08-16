package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class ResourceFilter extends DomainEntity {

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private ResourceFilter() {
        setPublicId(URI.create("urn:resource-filter:" + UUID.randomUUID()));
    }

    public static ResourceFilter create(Resource resource, Filter filter, Relevance relevance) {
        if (resource == null || filter == null || relevance == null) {
            throw new NullPointerException();
        }

        final var resourceFilter = new ResourceFilter();

        resourceFilter.filter = filter;
        resourceFilter.resource = resource;
        resourceFilter.relevance = relevance;

        filter.addResourceFilter(resourceFilter);
        resource.addResourceFilter(resourceFilter);
        relevance.addResourceFilter(resourceFilter);

        return resourceFilter;
    }

    public void disassociate() {
        final var resource = this.resource;
        final var filter = this.filter;
        final var relevance = this.relevance;

        this.resource = null;
        this.filter = null;
        this.relevance = null;

        if (resource != null) {
            resource.removeResourceFilter(this);
        }

        if (filter != null) {
            filter.removeResourceFilter(this);
        }

        if (relevance != null) {
            relevance.removeResourceFilter(this);
        }
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

    @PreRemove
    void preRemove() {
        this.disassociate();
    }

}
