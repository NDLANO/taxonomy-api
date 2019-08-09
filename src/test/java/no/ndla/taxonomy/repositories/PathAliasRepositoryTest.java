package no.ndla.taxonomy.repositories;

import no.ndla.taxonomy.domain.PathAlias;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.Entity;
import javax.transaction.Transactional;
import java.time.Instant;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("junit")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class PathAliasRepositoryTest {
    @Autowired
    private PathAliasRepository pathAliasRepository;

    static class PathAliasBuilder {
        String alias;
        String originalPath;
        String root;
        String leaf;
        Instant created;

        PathAlias build() {
            var pa = new PathAlias();
            pa.setAlias(alias);
            pa.setOriginalPath(originalPath);
            pa.setRoot(root);
            pa.setLeaf(leaf);
            pa.setCreated(created);
            return pa;
        }
    }

    @Before
    public void beforeTesting() {
        pathAliasRepository.deleteAll();
        pathAliasRepository.save(new PathAliasBuilder() {{
            alias = "test/alias-1";
            originalPath = "subject:1/topic:1/resource:1";
            root = "subject:1";
            leaf = "resource:1";
            created = Instant.ofEpochMilli(1000000000000L);
        }}.build());
    }

    @After
    public void afterTesting() {
        pathAliasRepository.deleteAll();
    }

    @Test
    public void testGetByAlias() {
        var optAlias = pathAliasRepository.findByAlias("test/alias-1");
        assertTrue(optAlias.isPresent());
        optAlias.ifPresent(alias -> {
            assertNotNull(alias.getId());
            assertEquals("subject:1/topic:1/resource:1", alias.getOriginalPath());
            assertEquals("subject:1", alias.getRoot());
            assertEquals("resource:1", alias.getLeaf());
            assertTrue(Math.abs(alias.getCreated().toEpochMilli() - 1000000000000L) < 2);
        });
    }

    @Test
    public void testFindAllByOriginalPath() {
        var aliases = pathAliasRepository.findAllByOriginalPath("subject:1/topic:1/resource:1");
        PathAlias alias;
        {
            assertNotNull(aliases);
            var iterator = aliases.iterator();
            assertTrue(iterator.hasNext());
            alias = iterator.next();
            assertFalse(iterator.hasNext());
        }
        assertEquals("test/alias-1", alias.getAlias());
    }
}
