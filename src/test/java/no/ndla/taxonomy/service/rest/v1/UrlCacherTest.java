package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Iterator;

import static no.ndla.taxonomy.service.TestUtils.assertAnyTrue;
import static no.ndla.taxonomy.service.TestUtils.count;
import static org.junit.Assert.assertEquals;

public class UrlCacherTest extends RestTest {

    @Autowired
    UrlCacher urlCacher;

    Subject subject1, subject2;
    Topic topic1, topic2;
    Resource resource1, resource2;

    /*
    (subject 1) ---> (topic 1) -> (resource 1)
               \____
                    \
                     v
    (subject 2) ---> (topic 2) -> (resource 2)
              primary

     */


    @Before
    public void before() throws Exception {
        resource1 = builder.resource(r -> r.publicId("urn:resource:1"));
        resource2 = builder.resource(r -> r.publicId("urn:resource:2"));

        topic1 = builder.topic(t -> t
                .publicId("urn:topic:1")
                .resource(resource1)
        );

        topic2 = builder.topic(t -> t
                .publicId("urn:topic:2")
                .resource(resource2)
        );


        subject1 = builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(topic1)
                .topic(topic2)
        );

        subject2 = builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(topic2)
        );

        topic2.setPrimarySubject(subject2);
        entityManager.flush();
    }

    @Test
    public void can_add_new_resource_to_cache() throws Exception {
        urlCacher.add(resource1);

        assertEquals(1, count(getUrls(resource1)));
        assertEquals("/subject:1/topic:1/resource:1", getUrls(resource1).next().getPath());
    }

    @Test
    public void old_urls_are_removed() throws Exception {
        cachedUrlRepository.save(new CachedUrl(resource1.getPublicId(), "/old/resource:1", true));

        urlCacher.add(resource1);

        assertEquals(1, count(getUrls(resource1)));
        assertEquals("/subject:1/topic:1/resource:1", getUrls(resource1).next().getPath());
    }

    @Test
    public void can_find_primary_path() throws Exception {
        urlCacher.add(resource2);

        Collection<CachedUrl> urls = cachedUrlRepository.findByPublicId(resource2.getPublicId());
        assertAnyTrue(urls, u -> u.isPrimary() && u.getPath().equals("/subject:2/topic:2/resource:2"));
        assertAnyTrue(urls, u -> !u.isPrimary() && u.getPath().equals("/subject:1/topic:2/resource:2"));
    }

    @Test
    public void removing_resource_deletes_all_urls_involving_resource() throws Exception {
        urlCacher.add(resource1);
        resourceRepository.delete(resource1);
        urlCacher.remove(resource1);
        Iterator<CachedUrl> urls = getUrls(resource1);

        assertEquals(0, count(urls));
    }

    @Test
    public void can_add_new_topic_to_cache() throws Exception {
        urlCacher.add(topic1);
        assertEquals(1, count(getUrls(topic1)));
        CachedUrl url = getUrls(topic1).next();
        assertEquals("/subject:1/topic:1", url.getPath());
    }

    @Test
    public void removing_topic_deletes_all_urls_involving_topic() throws Exception {
        urlCacher.add(topic1);
        assertEquals(1, count(getUrls(topic1)));

        topicRepository.delete(topic1);
        urlCacher.remove(topic1);

        assertEquals(0, count(getUrls(topic1)));
    }

    @Test
    public void relocating_topic_invalidates_and_regenerates_old_urls() throws Exception {
        urlCacher.add(subject1);
        urlCacher.add(topic1);
        urlCacher.add(resource1);

        subject1.removeTopic(topic1);
        subject2.addTopic(topic1);
        entityManager.flush();

        urlCacher.add(topic1);
        assertEquals(1, count(getUrls(topic1)));
        assertEquals("/subject:2/topic:1", getUrls(topic1).next().getPath());
    }

    private Iterator<CachedUrl> getUrls(DomainEntity domainEntity) {
        return cachedUrlRepository.findByPublicId(domainEntity.getPublicId()).iterator();
    }
}
