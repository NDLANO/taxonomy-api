package no.ndla.taxonomy.domain;


import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
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

    public TopicFilter(Topic topic, Filter filter, Relevance relevance) {
        setPublicId(URI.create("urn:topic-filter:" + UUID.randomUUID()));

        this.setFilter(filter);
        this.setTopic(topic);
        this.setRelevance(relevance);
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
        if (relevance != this.relevance && this.getRelevance() != null && this.getRelevance().getTopicFilters().contains(this)) {
            this.getRelevance().removeTopicFilter(this);
        }

        this.relevance = relevance;

        if (relevance != null && !relevance.getTopicFilters().contains(this)) {
            relevance.addTopicFilter(this);
        }
    }

    public void setFilter(Filter filter) {
        if (filter != this.filter && this.getFilter() != null && this.getFilter().getTopicFilters().contains(this)) {
            this.getFilter().removeTopicFilter(this);
        }

        this.filter = filter;

        if (filter != null && !filter.getTopicFilters().contains(this)) {
            filter.addTopicFilter(this);
        }
    }

    public void setTopic(Topic topic) {
        if (topic != this.topic && this.getTopic() != null && this.getTopic().getTopicFilters().contains(this)) {
            this.getTopic().removeTopicFilter(this);
        }

        this.topic = topic;

        if (topic != null && !topic.getTopicFilters().contains(this)) {
            topic.addTopicFilter(this);
        }
    }

    @PreRemove
    public void preRemove() {
        this.setTopic(null);
        this.setRelevance(null);
        this.setFilter(null);
    }

    public String toString() {
        return "TopicFilter: { " + topic.getName() + " " + topic.getPublicId() + " --" + relevance.getName() + "--> " + filter.getName() + " " + filter.getPublicId() + " }";
    }
}
