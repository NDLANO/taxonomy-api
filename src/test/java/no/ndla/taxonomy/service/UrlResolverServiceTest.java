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

import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
public class UrlResolverServiceTest {
    Builder builder;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UrlResolverService urlResolverService;

    @Before
    public void restTestSetUp() throws Exception {
        builder = new Builder(entityManager);
    }

    @Test
    public void resolveOldUrl() {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        CachedUrlOldRig cachedUrlOldRig = builder.cachedUrlOldRig(c -> c.oldUrl(oldUrl).public_id("topic:1:183926").subject_id("subject:11"));


        String path = urlResolverService.resolveOldUrl(oldUrl);

        assertEquals("service:11", path);
    }
}