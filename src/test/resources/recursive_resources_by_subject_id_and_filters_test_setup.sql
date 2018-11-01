-- Create a test structure with subjects, topics, subtopics and resources with different filters.
-- Of special interest in this structure are the resources that have filters that the parent topics do not have. (R:4 and R:5)
-- (S=subject, ST = subject-topic, TST = topic-subtopic,R = resource, F = filters)

-- S:1
--   - ST:1  (F:1, F:2)
--        - R:1  (F:1, F:2)
--        - R:2  (F:1, F:2)
--        - TST: 1-1  (F:1, F:2)
--              - R:3  (F:1, F:2)
--              - R:4  (F:1, F:2, F:3, F:4)
--              - R:5  (F:1, F:2, F:3, F:4)
--              - R:6  (F:1, F:2)

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

insert into filter(id, public_id, subject_id, name)
VALUES (1, 'urn:filter:1', 1, 'F:1'),
       (2, 'urn:filter:2', 1, 'F:2'),
       (3, 'urn:filter:3', 1, 'F:3'),
       (3, 'urn:filter:4', 1, 'F:4');

insert into relevance (id, public_id, name) VALUES (1, 'urn:relevance:core', 'core');

insert into topic_filter (id, public_id, topic_id, filter_id, relevance_id)
VALUES  (1, 'urn:topic-filter:1', 1, 1, 1),
        (2, 'urn:topic-filter:2', 1, 2, 1),
        (3, 'urn:topic-filter:3', 2, 1, 1),
        (4, 'urn:topic-filter:4', 2, 2, 1);

insert into resource_filter (id, public_id, resource_id, filter_id, relevance_id)
VALUES (1, 'urn:resource-filter:1', 1, 1, 1),
       (2, 'urn:resource-filter:2', 1, 2, 1),

       (3, 'urn:resource-filter:3', 2, 1, 1),
       (4, 'urn:resource-filter:4', 2, 2, 1),

       (5, 'urn:resource-filter:5', 3, 1, 1),
       (6, 'urn:resource-filter:6', 3, 2, 1),

       (7, 'urn:resource-filter:7', 4, 1, 1),
       (8, 'urn:resource-filter:8', 4, 2, 1),
       (9, 'urn:resource-filter:9', 4, 3, 1),
       (10, 'urn:resource-filter:10', 4, 4, 1),

       (11, 'urn:resource-filter:11', 5, 1, 1),
       (12, 'urn:resource-filter:12', 5, 2, 1),
       (13, 'urn:resource-filter:13', 5, 3, 1),
       (14, 'urn:resource-filter:14', 5, 4, 1),

       (15, 'urn:resource-filter:15', 6, 1, 1),
       (16, 'urn:resource-filter:16', 6, 2, 1);
