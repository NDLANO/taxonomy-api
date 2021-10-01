/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.commands.SubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
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
        builder.subject(s -> s
                .name("english")
                .contentUri("urn:article:1")
                .publicId("urn:subject:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1");
        SubjectIndexDocument subject = testUtils.getObject(SubjectIndexDocument.class, response);

        assertEquals("english", subject.name);
        assertEquals("urn:article:1", subject.contentUri.toString());
        assertEquals("/subject:1", subject.path);

        assertNotNull(subject.getMetadata());
        assertTrue(subject.getMetadata().isVisible());
        assertTrue(subject.getMetadata().getGrepCodes().size() == 1 && subject.getMetadata().getGrepCodes().contains("SUBJECT1"));
    }

    @Test
    public void can_get_all_subjects() throws Exception {
        builder.subject(s -> s.name("english"));
        builder.subject(s -> s.name("mathematics"));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects");
        SubjectIndexDocument[] subjects = testUtils.getObject(SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
        assertAllTrue(subjects, s -> !s.path.isEmpty());

        assertAllTrue(subjects, s -> s.getMetadata() != null);

        assertAllTrue(subjects, s -> s.getMetadata().isVisible());
        assertAllTrue(subjects, s -> s.getMetadata().getGrepCodes().size() == 1);
    }

    @Test
    public void can_create_subject() throws Exception {
        final var createSubjectCommand = new SubjectCommand() {{
            name = "testsubject";
            contentUri = URI.create("urn:article:1");
        }};

        MockHttpServletResponse response = testUtils.createResource("/v1/subjects", createSubjectCommand);
        URI id = getId(response);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(createSubjectCommand.name, subject.getName());
        assertEquals(createSubjectCommand.contentUri, subject.getContentUri());
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        final var command = new SubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        MockHttpServletResponse response = testUtils.createResource("/v1/subjects", command);
        assertEquals("/v1/subjects/urn:subject:1", response.getHeader("Location"));

        assertNotNull(subjectRepository.getByPublicId(command.id));
    }

    @Test
    public void can_update_subject() throws Exception {
        URI publicId = builder.subject().getPublicId();

        final var command = new SubjectCommand() {{
            id = publicId;
            name = "physics";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Subject subject = subjectRepository.getByPublicId(publicId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
    }

    @Test
    public void can_update_subject_with_new_id() throws Exception {
        URI publicId = builder.subject().getPublicId();
        URI randomId = URI.create("urn:subject:random");

        final var command = new SubjectCommand() {{
            id = randomId;
            name = "random";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/subjects/" + publicId, command);

        Subject subject = subjectRepository.getByPublicId(randomId);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        final var command = new SubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        testUtils.createResource("/v1/subjects", command, status().isCreated());
        testUtils.createResource("/v1/subjects", command, status().isConflict());
    }

    @Test
    public void can_delete_subject() throws Exception {
        URI id = builder.subject(s -> s
                .topic(t -> t.publicId("urn:topic:1"))
                .translation("nb", tr -> tr.name("fag"))
        ).getPublicId();

        testUtils.deleteResource("/v1/subjects/" + id);
        assertNull(subjectRepository.findByPublicId(id));

        verify(metadataApiService).deleteMetadataByPublicId(id);
    }

    @Test
    public void can_get_topics() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").contentUri("urn:article:1"))
                .topic(t -> t.name("electricity").contentUri("urn:article:2"))
                .topic(t -> t.name("optics").contentUri("urn:article:3"))
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

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
        URI subjectid = builder.subject("subject", s -> s
                .name("subject")
                .publicId("urn:subject:1")
                .topic("parent", parent -> parent
                        .name("parent topic")
                        .publicId("urn:topic:a")
                        .subtopic("child", child -> child
                                .name("child topic")
                                .publicId("urn:topic:aa")
                                .subtopic("grandchild", grandchild -> grandchild
                                        .name("grandchild topic")
                                        .publicId("urn:topic:aaa")
                                )
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

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

        Subject subject = builder.subject("subject");
        assertEquals(first(subject.getSubjectTopics()).getPublicId(), topics[0].connectionId);

        Topic parent = builder.topic("parent");
        assertEquals(first(parent.getChildrenTopicSubtopics()).getPublicId(), topics[1].connectionId);

        Topic child = builder.topic("child");
        assertEquals(first(child.getChildrenTopicSubtopics()).getPublicId(), topics[2].connectionId);
    }


    @Test
    public void recursive_topics_are_ordered_by_rank_relative_to_parent() throws Exception {
        testSeeder.recursiveTopicsBySubjectIdTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);
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
    public void recursive_topics_with_filter_are_ordered_relative_to_parent() throws Exception {
        testSeeder.recursiveTopicsBySubjectIdAndFiltersTestSetup();
        //test filter 1
        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true&filter=urn:filter:1");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);
        assertEquals(0, topics.length); // Filters are removed

        //test filter 2
        MockHttpServletResponse response2 = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true&filter=urn:filter:2");
        SubTopicIndexDocument[] topics2 = testUtils.getObject(SubTopicIndexDocument[].class, response2);
        assertEquals(0, topics2.length); // Filters are removed
    }
}
