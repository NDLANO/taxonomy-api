/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.service.dtos.EntityWithPathDTO;
import no.ndla.taxonomy.service.dtos.NodeChildDTO;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.SubjectChildDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubjectsTest extends RestTest {
    @Autowired
    private TestSeeder testSeeder;

    @Test
    public void can_get_single_subject() throws Exception {
        Version version = versionService.getPublished().get();
        builder.node(NodeType.SUBJECT, version,
                s -> s.isContext(true).name("english").contentUri("urn:article:1").publicId("urn:subject:1"));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1");
        NodeDTO subject = testUtils.getObject(NodeDTO.class, response);

        assertEquals("english", subject.getName());
        assertEquals("urn:article:1", subject.getContentUri().toString());
        assertEquals("/subject:1", subject.getPath());

        assertNotNull(subject.getMetadata());
        assertTrue(subject.getMetadata().isVisible());
        assertTrue(subject.getMetadata().getGrepCodes().size() == 1
                && subject.getMetadata().getGrepCodes().contains("SUBJECT1"));
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        Version version = versionService.getPublished().get();
        builder.node(NodeType.SUBJECT, version, s -> s.isContext(true).name("english"));
        builder.node(NodeType.SUBJECT, version, s -> s.isContext(true).name("mathematics"));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects");
        NodeDTO[] subjects = testUtils.getObject(NodeDTO[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.getName()));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.getName()));
        assertAllTrue(subjects, s -> isValidId(s.getId()));
        assertAllTrue(subjects, s -> !s.getPath().isEmpty());

        assertAllTrue(subjects, s -> s.getMetadata() != null);

        assertAllTrue(subjects, s -> s.getMetadata().isVisible());
        assertAllTrue(subjects, s -> s.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createSubjectCommand = new SubjectCommand() {
            {
                name = "testsubject";
                contentUri = URI.create("urn:article:1");
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/subjects", createSubjectCommand);
        URI id = getId(response);

        Node subject = nodeRepository.getByPublicId(id);
        assertEquals(createSubjectCommand.name, subject.getName());
        assertEquals(createSubjectCommand.contentUri, subject.getContentUri());
        assertTrue(subject.isRoot()); // all subjects are roots
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        final var command = new SubjectCommand() {
            {
                id = URI.create("urn:subject:1");
                name = "name";
            }
        };

        MockHttpServletResponse response = testUtils.createResource("/v1/subjects", command);
        assertEquals("/v1/subjects/urn:subject:1", response.getHeader("Location"));

        assertNotNull(nodeRepository.getByPublicId(command.id));
    }

    @Test
    public void can_update_subject() throws Exception {
        Version version = versionService.getBeta().get();
        URI publicId = builder.node(NodeType.SUBJECT, version).getPublicId();

        final var command = new SubjectCommand() {
            {
                id = publicId;
                name = "physics";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Node subject = nodeRepository.getByPublicId(publicId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
    }

    @Test
    public void can_update_subject_with_new_id() throws Exception {
        Version version = versionService.getBeta().get();
        URI publicId = builder.node(NodeType.SUBJECT, version).getPublicId();
        URI randomId = URI.create("urn:subject:random");

        final var command = new SubjectCommand() {
            {
                id = randomId;
                name = "random";
                contentUri = URI.create("urn:article:1");
            }
        };

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Node subject = nodeRepository.getByPublicId(randomId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new SubjectCommand() {
            {
                id = URI.create("urn:subject:1");
                name = "name";
            }
        };

        testUtils.createResource("/v1/subjects", command, status().isCreated());
        testUtils.createResource("/v1/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        Version version = versionService.getBeta().get();
        URI id = builder.node(NodeType.SUBJECT, version, s -> s
                .child(NodeType.TOPIC, version, t -> t.publicId("urn:topic:1")).translation("nb", tr -> tr.name("fag")))
                .getPublicId();

        testUtils.deleteResource("/v1/subjects/" + id);
        assertNull(nodeRepository.findByPublicId(id));

        verify(metadataApiService).deleteMetadataByPublicId(id);
    }

    @Test
    public void can_get_topics() throws Exception {
        Version version = versionService.getPublished().get();
        Node subject = builder.node(NodeType.SUBJECT, version,
                s -> s.isContext(true).name("physics")
                        .child(NodeType.TOPIC, version, t -> t.name("statics").contentUri("urn:article:1"))
                        .child(NodeType.TOPIC, version, t -> t.name("electricity").contentUri("urn:article:2"))
                        .child(NodeType.TOPIC, version, t -> t.name("optics").contentUri("urn:article:3")));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        NodeChildDTO[] topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, topics.length);
        assertAnyTrue(topics, t -> "statics".equals(t.name) && "urn:article:1".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "electricity".equals(t.name) && "urn:article:2".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> "optics".equals(t.name) && "urn:article:3".equals(t.contentUri.toString()));
        assertAnyTrue(topics, t -> t.isPrimary);
        assertAllTrue(topics, t -> isValidId(t.id));
        assertAllTrue(topics, t -> isValidId(t.connectionId));
        assertAllTrue(topics, t -> !t.path.isEmpty());
        assertAllTrue(topics, t -> t.parent.equals(subject.getPublicId()));

        assertAllTrue(topics, t -> t.getMetadata() != null);

        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_topics_recursively() throws Exception {
        Version version = versionService.getPublished().get();
        URI subjectId = builder
                .node("subject", NodeType.SUBJECT, version,
                        s -> s.isContext(true).name("subject").publicId("urn:subject:1").child("parent", NodeType.TOPIC,
                                version,
                                parent -> parent.name("parent topic").publicId("urn:topic:a").child(
                                        "child", NodeType.TOPIC, version,
                                        child -> child.name("child topic").publicId("urn:topic:aa").child("grandchild",
                                                NodeType.TOPIC, version, grandchild -> grandchild
                                                        .name("grandchild topic").publicId("urn:topic:aaa")))))
                .getPublicId();

        MockHttpServletResponse response = testUtils
                .getResource("/v1/subjects/" + subjectId + "/topics?recursive=true");
        NodeChildDTO[] topics = testUtils.getObject(NodeChildDTO[].class, response);

        assertEquals(3, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("/subject:1/topic:a", topics[0].path);
        assertEquals("child topic", topics[1].name);
        assertEquals("/subject:1/topic:a/topic:aa", topics[1].path);
        assertEquals("grandchild topic", topics[2].name);
        assertEquals("/subject:1/topic:a/topic:aa/topic:aaa", topics[2].path);

        assertAllTrue(topics, t -> t.getMetadata() != null);

        assertAllTrue(topics, t -> t.getMetadata().isVisible());
        assertAllTrue(topics, t -> t.getMetadata().getGrepCodes().size() == 1);

        Node subject = builder.node("subject", version);
        assertEquals(first(subject.getChildConnections()).getPublicId(), topics[0].connectionId);

        Node parent = builder.node("parent", version);
        assertEquals(first(parent.getChildConnections()).getPublicId(), topics[1].connectionId);

        Node child = builder.node("child", version);
        assertEquals(first(child.getChildConnections()).getPublicId(), topics[2].connectionId);
    }

    @Test
    public void recursive_topics_are_ordered_by_rank_relative_to_parent() throws Exception {
        testSeeder.recursiveNodesBySubjectNodeIdTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        SubjectChildDTO[] topics = testUtils.getObject(SubjectChildDTO[].class, response);
        assertEquals(8, topics.length);
        assertEquals("urn:topic:1", topics[0].id.toString());
        assertEquals("urn:topic:2", topics[1].id.toString());
        assertEquals("urn:topic:3", topics[2].id.toString());
        assertEquals("urn:topic:4", topics[3].id.toString());
        assertEquals("urn:topic:5", topics[4].id.toString());
        assertEquals("urn:topic:6", topics[5].id.toString());
        assertEquals("urn:topic:7", topics[6].id.toString());
        assertEquals("urn:topic:8", topics[7].id.toString());
    }

    @Test
    public void recursive_topics_with_relevance_are_ordered_relative_to_parent() throws Exception {
        testSeeder.recursiveNodesBySubjectNodeIdAndRelevanceTestSetup();
        // test core relevance
        MockHttpServletResponse response = testUtils
                .getResource("/v1/subjects/urn:subject:1/topics?recursive=true&relevance=urn:relevance:core");
        SubjectChildDTO[] topics = testUtils.getObject(SubjectChildDTO[].class, response);
        assertEquals(5, topics.length);

        // test supplementary relevance
        MockHttpServletResponse response2 = testUtils
                .getResource("/v1/subjects/urn:subject:1/topics?recursive=true&relevance=urn:relevance:supplementary");
        SubjectChildDTO[] topics2 = testUtils.getObject(SubjectChildDTO[].class, response2);
        assertEquals(4, topics2.length);
    }
}
