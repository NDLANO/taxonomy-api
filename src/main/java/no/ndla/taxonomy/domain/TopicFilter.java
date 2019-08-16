package no.ndla.taxonomy.domain;


import javax.persistence.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class TopicFilter extends DomainEntity {

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "relevance_id")
    private Relevance relevance;

    private TopicFilter() {
        setPublicId(URI.create("urn:topic-filter:" + UUID.randomUUID()));
    }

    public static TopicFilter create(Topic topic, Filter filter, Relevance relevance) {
        if (topic == null || filter == null || relevance == null) {
            throw new NullPointerException();
        }

        final var topicFilter = new TopicFilter();

        topicFilter.filter = filter;
        topicFilter.topic = topic;
        topicFilter.relevance = relevance;

        filter.addTopicFilter(topicFilter);
        topic.addTopicFilter(topicFilter);
        relevance.addTopicFilter(topicFilter);

        return topicFilter;
    }

    public void disassociate() {
        final var topic = this.topic;
        final var filter = this.filter;
        final var relevance = this.relevance;

        this.topic = null;
        this.filter = null;
        this.relevance = null;

        if (topic != null) {
            topic.removeTopicFilter(this);
        }

        if (filter != null) {
            filter.removeTopicFilter(this);
        }

        if (relevance != null) {
            relevance.removeTopicFilter(this);
        }
    }

    public Optional<Filter> getFilter() {
        return Optional.ofNullable(filter);
    }

    public Optional<Topic> getTopic() {
        return Optional.ofNullable(topic);
    }

    public Optional<Relevance> getRelevance() {
        return Optional.ofNullable(relevance);
    }

    @PreRemove
    public void preRemove() {
        this.disassociate();
    }
}
