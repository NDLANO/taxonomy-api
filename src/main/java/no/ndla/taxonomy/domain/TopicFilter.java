package no.ndla.taxonomy.domain;


import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.UUID;

@Entity
public class TopicFilter extends DomainEntity {

    @ManyToOne
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    protected TopicFilter() {
    }

    public TopicFilter(Topic topic, Filter filter, Relevance relevance, URI publicId) {
        this.filter = filter;
        this.topic = topic;
        this.relevance = relevance;
        setPublicId(publicId);
    }

    public Filter getFilter() {
        return filter;
    }

    public Topic getTopic() {
        return topic;
    }

    public Relevance getRelevance() {
        return relevance;
    }

    public void setRelevance(Relevance relevance) {
        this.relevance = relevance;
    }

    public String toString() {
        return "TopicFilter: { " + topic.getName() + " " + topic.getPublicId() + " --" + relevance.getName() + "--> " + filter.getName() + " " + filter.getPublicId() + " }";
    }
}
