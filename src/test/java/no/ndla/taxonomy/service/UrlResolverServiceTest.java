package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.CachedUrlOldRig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
/*
 * Test Service and database together.
 */
public class UrlResolverServiceTest {
    private Builder builder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UrlResolverService urlResolverService;

    @Before
    public void restTestSetUp() {
        builder = new Builder(entityManager);
    }

    @Test
    @Transactional
    public void resolveOldUrl() {
        builder.subject(s -> s
                .publicId("urn:subject:11")
                .topic(t -> t
                        .publicId("urn:topic:1:183926")
                )
        );

        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        CachedUrlOldRig cachedUrlOldRig = builder.cachedUrlOldRig(c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11"));
        entityManager.persist(cachedUrlOldRig);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl);

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlNoSubjectPrimaryPath() {
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId("urn:topic:1:183926")
                )
        );

        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        CachedUrlOldRig cachedUrlOldRig = builder.cachedUrlOldRig(c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926"));
        entityManager.persist(cachedUrlOldRig);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlBadSubjectPrimaryPath() {
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId("urn:topic:1:183926")
                )
        );

        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        CachedUrlOldRig cachedUrlOldRig = builder.cachedUrlOldRig(c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11"));
        entityManager.persist(cachedUrlOldRig);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }


}