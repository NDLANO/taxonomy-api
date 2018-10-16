-- Create a test structure with subjects, topics, subtopics and resources
-- (S=subject, ST = subject-topic, TST = topic-subtopic,R = resource)

-- S:1
--   - ST:1
--        - R:1
--        - R:2
--        - TST: 1-1
--              - R:3
--              - R:4
--              - R:5
--              - R:6

insert into subject (id, public_id, name, content_uri)
VALUES (1, 'urn:subject:1', 'S:1', null);

insert into topic (id, public_id, name, content_uri, context)
VALUES (1, 'urn:topic:1', 'ST:1', null, false),
       (2, 'urn:topic:2', 'TST:1-1', null, false);

insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1);

insert into topic_subtopic (id, public_id, topic_id, subtopic_id, is_primary, rank)
VALUES (1, 'urn:topic-subtopic:1', 1, 2, true, 1);

insert into resource(id, public_id, name, content_uri)
VALUES (1, 'urn:resource:1', 'R:1', null),
       (2, 'urn:resource:2', 'R:2', null),
       (3, 'urn:resource:3', 'R:3', null),
       (4, 'urn:resource:4', 'R:4', null),
       (5, 'urn:resource:5', 'R:5', null),
       (6, 'urn:resource:6', 'R:6', null);

insert into topic_resource(id, public_id, topic_id, resource_id, is_primary, rank)
VALUES (1, 'urn:topic-resource:1', 1, 1, true, 1),
       (2, 'urn:topic-resource:2', 1, 2, true, 2),
       (3, 'urn:topic-resource:3', 2, 3, true, 1),
       (4, 'urn:topic-resource:4', 2, 4, true, 2),
       (5, 'urn:topic-resource:5', 2, 5, true, 3),
       (6, 'urn:topic-resource:6', 2, 6, true, 4);