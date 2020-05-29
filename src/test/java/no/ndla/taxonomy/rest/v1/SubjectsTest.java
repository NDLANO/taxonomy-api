package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.TestSeeder;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.rest.v1.commands.CreateSubjectCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateSubjectCommand;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubTopicIndexDocument;
import no.ndla.taxonomy.rest.v1.dtos.subjects.SubjectIndexDocument;
import no.ndla.taxonomy.service.dtos.ResourceDTO;
import no.ndla.taxonomy.service.dtos.ResourceWithTopicConnectionDTO;
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

        assertNull(subject.metadata);
    }

    @Test
    public void can_get_single_subject_with_metadata() throws Exception {
        builder.subject(s -> s
                .name("english")
                .contentUri("urn:article:1")
                .publicId("urn:subject:1")
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1?includeMetadata=true");
        SubjectIndexDocument subject = testUtils.getObject(SubjectIndexDocument.class, response);

        assertEquals("english", subject.name);
        assertEquals("urn:article:1", subject.contentUri.toString());
        assertEquals("/subject:1", subject.path);

        assertNotNull(subject.metadata);
        assertTrue(subject.metadata.isVisible());
        assertTrue(subject.metadata.getGrepCodes().size() == 1 && subject.metadata.getGrepCodes().contains("SUBJECT1"));
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

        assertAllTrue(subjects, s -> s.metadata == null);
    }

    @Test
    public void can_get_all_subjects_with_metadata() throws Exception {
        builder.subject(s -> s.name("english"));
        builder.subject(s -> s.name("mathematics"));

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects?includeMetadata=true");
        SubjectIndexDocument[] subjects = testUtils.getObject(SubjectIndexDocument[].class, response);
        assertEquals(2, subjects.length);

        assertAnyTrue(subjects, s -> "english".equals(s.name));
        assertAnyTrue(subjects, s -> "mathematics".equals(s.name));
        assertAllTrue(subjects, s -> isValidId(s.id));
        assertAllTrue(subjects, s -> !s.path.isEmpty());

        assertAllTrue(subjects, s -> s.metadata != null);

        assertAllTrue(subjects, s -> s.metadata.isVisible());
        assertAllTrue(subjects, s -> s.metadata.getGrepCodes().size() == 1);
    }

    @Test
    public void can_create_subject() throws Exception {
        CreateSubjectCommand createSubjectCommand = new CreateSubjectCommand() {{
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
    public void can_update_subject() throws Exception {
        URI id = builder.subject().getPublicId();

        UpdateSubjectCommand command = new UpdateSubjectCommand() {{
            name = "physics";
            contentUri = URI.create("urn:article:1");
        }};

        testUtils.updateResource("/v1/subjects/" + id, command);

        Subject subject = subjectRepository.getByPublicId(id);
        assertEquals(command.name, subject.getName());
        assertEquals(command.contentUri, subject.getContentUri());
    }

    @Test
    public void can_create_subject_with_id() throws Exception {
        CreateSubjectCommand command = new CreateSubjectCommand() {{
            id = URI.create("urn:subject:1");
            name = "name";
        }};

        MockHttpServletResponse response = testUtils.createResource("/v1/subjects", command);
        assertEquals("/v1/subjects/urn:subject:1", response.getHeader("Location"));

        assertNotNull(subjectRepository.getByPublicId(command.id));
    }

    @Test
    public void duplicate_ids_not_allowed() throws Exception {
        CreateSubjectCommand command = new CreateSubjectCommand() {{
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
                .filter(f -> f.publicId("urn:filter:1"))
        ).getPublicId();

        testUtils.deleteResource("/v1/subjects/" + id);
        assertNull(subjectRepository.findByPublicId(id));
        assertNull(filterRepository.findByPublicId(URI.create("urn:filter:1")));

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

        assertAllTrue(topics, t -> t.metadata == null);
    }

    @Test
    public void can_get_topics_with_metadata() throws Exception {
        Subject subject = builder.subject(s -> s
                .name("physics")
                .topic(t -> t.name("statics").contentUri("urn:article:1"))
                .topic(t -> t.name("electricity").contentUri("urn:article:2"))
                .topic(t -> t.name("optics").contentUri("urn:article:3"))
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subject.getPublicId() + "/topics?includeMetadata=true");
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

        assertAllTrue(topics, t -> t.metadata != null);

        assertAllTrue(topics, t -> t.metadata.isVisible());
        assertAllTrue(topics, t -> t.metadata.getGrepCodes().size() == 1);
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

        assertAllTrue(topics, t -> t.metadata == null);

        Subject subject = builder.subject("subject");
        assertEquals(first(subject.getSubjectTopics()).getPublicId(), topics[0].connectionId);

        Topic parent = builder.topic("parent");
        assertEquals(first(parent.getChildrenTopicSubtopics()).getPublicId(), topics[1].connectionId);

        Topic child = builder.topic("child");
        assertEquals(first(child.getChildrenTopicSubtopics()).getPublicId(), topics[2].connectionId);
    }

    @Test
    public void can_get_topics_recursively_with_metadata() throws Exception {
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

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + subjectid + "/topics?recursive=true&includeMetadata=true");
        SubTopicIndexDocument[] topics = testUtils.getObject(SubTopicIndexDocument[].class, response);

        assertEquals(3, topics.length);
        assertEquals("parent topic", topics[0].name);
        assertEquals("/subject:1/topic:a", topics[0].path);
        assertEquals("child topic", topics[1].name);
        assertEquals("/subject:1/topic:a/topic:aa", topics[1].path);
        assertEquals("grandchild topic", topics[2].name);
        assertEquals("/subject:1/topic:a/topic:aa/topic:aaa", topics[2].path);

        assertAllTrue(topics, t -> t.metadata != null);

        assertAllTrue(topics, t -> t.metadata.isVisible());
        assertAllTrue(topics, t -> t.metadata.getGrepCodes().size() == 1);

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
        assertEquals(5, topics.length);
        assertEquals("urn:topic:1", topics[0].id.toString());
        assertEquals("urn:topic:2", topics[1].id.toString());
        assertEquals("urn:topic:5", topics[2].id.toString());
        assertEquals("urn:topic:6", topics[3].id.toString());
        assertEquals("urn:topic:7", topics[4].id.toString());

        //test filter 2
        MockHttpServletResponse response2 = testUtils.getResource("/v1/subjects/urn:subject:1/topics?recursive=true&filter=urn:filter:2");
        SubTopicIndexDocument[] topics2 = testUtils.getObject(SubTopicIndexDocument[].class, response2);
        assertEquals(4, topics2.length);
        assertEquals("urn:topic:3", topics2[0].id.toString());
        assertEquals("urn:topic:4", topics2[1].id.toString());
        assertEquals("urn:topic:5", topics2[2].id.toString());
        assertEquals("urn:topic:8", topics2[3].id.toString());
    }

    @Test
    public void resources_are_ordered_relative_to_parent() throws Exception {
        testSeeder.resourcesBySubjectIdTestSetup();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(10, resources.length);
        assertEquals("R:9", resources[0].name);
        assertEquals("R:1", resources[1].name);
        assertEquals("R:2", resources[2].name);
        assertEquals("R:10", resources[3].name);
        assertEquals("R:3", resources[4].name);
        assertEquals("R:5", resources[5].name);
        assertEquals("R:4", resources[6].name);
        assertEquals("R:6", resources[7].name);
        assertEquals("R:7", resources[8].name);
        assertEquals("R:8", resources[9].name);
    }

    @Test
    public void filtered_resources_are_ordered_relative_to_parent() throws Exception {
        testSeeder.resourcesBySubjectIdTestSetup();

        //filter 1
        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/resources?filter=urn:filter:1");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(5, resources.length);
        assertEquals("R:9", resources[0].name);
        assertEquals("R:1", resources[1].name);
        assertEquals("R:3", resources[2].name);
        assertEquals("R:5", resources[3].name);
        assertEquals("R:7", resources[4].name);

        //filter 2
        MockHttpServletResponse response2 = testUtils.getResource("/v1/subjects/urn:subject:1/resources?filter=urn:filter:2");
        final var resources2 = testUtils.getObject(ResourceDTO[].class, response2);

        assertEquals(5, resources2.length);
        assertEquals("R:2", resources2[0].name);
        assertEquals("R:10", resources2[1].name);
        assertEquals("R:4", resources2[2].name);
        assertEquals("R:6", resources2[3].name);
        assertEquals("R:8", resources2[4].name);
    }


    @Test
    public void resources_can_have_content_uri() throws Exception {
        URI id = builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .resource(r -> r.contentUri("urn:article:1"))
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals("urn:article:1", resources[0].contentUri.toString());
    }

    @Test
    public void resources_can_have_filters() throws Exception {
        Relevance relevance = builder.relevance(r -> r.publicId("urn:relevance:core"));
        Filter filter = builder.filter(f -> f.publicId("urn:filter:vg1"));

        URI id = builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .resource(r -> r
                                .contentUri("urn:article:1")
                                .filter(filter, relevance)
                        )
                )
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals("urn:filter:vg1", first(resources[0].filters).getId().toString());
        assertEquals("urn:relevance:core", first(resources[0].filters).getRelevanceId().toString());
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively() throws Exception {
        URI id = builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("subject")
                .topic("topic a", t -> t
                        .name("topic a")
                        .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                .topic("topic b", t -> t
                        .name("topic b")
                        .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                        .subtopic("subtopic", st -> st.name("subtopic").resource(r -> r.name("sub resource"))))
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources");
        final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(3, resources.length);

        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic a").getTopicResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic b").getTopicResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("subtopic").getTopicResources()).getPublicId()));

        assertAllTrue(resources, r -> r.metadata == null);
    }

    @Test
    public void can_get_resources_for_a_subject_and_its_topics_recursively_with_metadata() throws Exception {
        URI id = builder.subject(s -> s
                .publicId("urn:subject:1")
                .name("subject")
                .topic("topic a", t -> t
                        .name("topic a")
                        .resource(r -> r.name("resource a").resourceType(rt -> rt.name("assignment"))))
                .topic("topic b", t -> t
                        .name("topic b")
                        .resource(r -> r.name("resource b").resourceType(rt -> rt.name("lecture")))
                        .subtopic("subtopic", st -> st.name("subtopic").resource(r -> r.name("sub resource"))))
        ).getPublicId();

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/" + id + "/resources?includeMetadata=true");
        final var resources = testUtils.getObject(ResourceWithTopicConnectionDTO[].class, response);

        assertEquals(3, resources.length);

        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic a").getTopicResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("topic b").getTopicResources()).getPublicId()));
        assertAnyTrue(resources, r -> r.connectionId.equals(first(builder.topic("subtopic").getTopicResources()).getPublicId()));

        assertAllTrue(resources, r -> r.metadata != null);
        assertAllTrue(resources, r -> r.metadata.isVisible());
        assertAllTrue(resources, r -> r.metadata.getGrepCodes().size() == 1);
    }

    @Test
    public void can_get_urls_for_all_resources() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(true, r -> r.publicId("urn:resource:1"))
                )
                .topic(t -> t
                        .publicId("urn:topic:2")
                        .resource(true, r -> r.publicId("urn:resource:2"))
                        .subtopic(st -> st
                                .publicId("urn:topic:21")
                                .resource(true, r -> r.publicId("urn:resource:3"))
                        )
                )
        );

        MockHttpServletResponse response = testUtils.getResource("/v1/subjects/urn:subject:1/resources");
        final var resources = testUtils.getObject(ResourceDTO[].class, response);

        assertEquals(3, resources.length);
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:1/resource:1"));
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:2/resource:2"));
        assertAnyTrue(resources, r -> r.path.equals("/subject:1/topic:2/topic:21/resource:3"));
    }
}
