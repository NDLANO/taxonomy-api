package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicFilter extends DomainEntity {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    public TopicFilter() {
        setPublicId(URI.create("urn:topic-filter:" + UUID.randomUUID()));
    }

    public Optional<Filter> getFilter() {
        return Optional.ofNullable(filter);
    }

    public void setFilter(Filter filter) {
        if (filter != this.filter && this.filter != null && this.filter.getTopicFilters().contains(this)) {
            this.filter.removeTopicFilter(this);
        }

        this.filter = filter;

        if (filter != null && !filter.getTopicFilters().contains(this)) {
            filter.addTopicFilter(this);
        }
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public void setTopic(Topic topic) {
        if (topic != this.topic && this.topic != null && this.topic.getTopicFilters().contains(this)) {
            this.topic.removeTopicFilter(this);
        }

        this.topic = topic;

        if (topic != null && !topic.getTopicFilters().contains(this)) {
            topic.addTopicFilter(this);
        }
    }

    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    public void setRelevance(Relevance relevance) {
        if (relevance != this.relevance && this.relevance != null && this.relevance.getTopicFilters().contains(this)) {
            this.relevance.removeTopicFilter(this);
        }

        this.relevance = relevance;

        if (relevance != null && !relevance.getTopicFilters().contains(this)) {
            relevance.addTopicFilter(this);
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
