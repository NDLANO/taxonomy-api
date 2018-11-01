-- create a test structure with subjects, topics, subtopics and resources as follows
-- (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)

-- S:1
--   - ST:1
--      - R:9 (F:1)
--        - TST: 1-1
--            - R:1 (F:1)
--   - ST:2
--        - TST:2-1
--            - R:2 (F:2)
--            - TST: 2-1-1
--                  - R:10 (F:2)
--   - ST:3
--        - TST:3-1
--            - R:3 (F:1)
--            - R:5 (F:1)
--            - R:4 (F:2)
--        - TST:3-2
--            - R:6 (F:2)
--        - TST:3-3
--            - R:7 (F:1)
--            - R:8 (F:2)

insert into subject (id, public_id, name, content_uri)
VALUES (1, 'urn:subject:1', 'S:1', null);

insert into topic (id, public_id, name, content_uri, context)
VALUES (1, 'urn:topic:1', 'ST:1', null, false),
       (2, 'urn:topic:2', 'TST:1-1', null, false),
       (3, 'urn:topic:3', 'ST:2', null, false),
       (4, 'urn:topic:4', 'TST:2-1', null, false),
       (5, 'urn:topic:5', 'ST:3', null, false),
       (6, 'urn:topic:6', 'TST:3-1', null, false),
       (7, 'urn:topic:7', 'TST:3-2', null, false),
       (8, 'urn:topic:8', 'TST:3-3', null, false),
       (9, 'urn:topic:9', 'TST:2-1-1', null, false);


insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1),
       (2, 'urn:subject-topic:2', 3, 1, true, 2),
       (3, 'urn:subject-topic-3', 5, 1, true, 3);

insert into topic_subtopic (id, public_id, topic_id, subtopic_id, is_primary, rank)
VALUES (1, 'urn:topic-subtopic:1', 1, 2, true, 1),
       (2, 'urn:topic-subtopic:2', 3, 4, true, 1),
       (3, 'urn:topic-subtopic:3', 5, 6, true, 1),
       (4, 'urn:topic-subtopic:4', 5, 7, true, 2),
       (5, 'urn:topic-subtopic:5', 5, 8, true, 3),
       (6, 'urn:topic-subtopic:6', 4, 9, true, 1);

insert into resource (id, public_id, name, content_uri)
VALUES (1, 'urn:resource:1', 'R:1', null),
       (2, 'urn:resource:2', 'R:2', null),
       (3, 'urn:resource:3', 'R:3', null),
       (4, 'urn:resource:4', 'R:4', null),
       (5, 'urn:resource:5', 'R:5', null),
       (6, 'urn:resource:6', 'R:6', null),
       (7, 'urn:resource:7', 'R:7', null),
       (8, 'urn:resource:8', 'R:8', null),
       (9, 'urn:resource:9', 'R:9', null),
       (10, 'urn:resource:10', 'R:10', null);

insert into topic_resource(id, public_id, topic_id, resource_id, is_primary, rank)
VALUES (1, 'urn:topic-resource:1', 2, 1, true, 1),
       (2, 'urn:topic-resource:2', 4, 2, true, 1),
       (3, 'urn:topic-resource:3', 6, 3, true, 1),
       (4, 'urn:topic-resource:4', 6, 4, true, 3),
       (5, 'urn:topic-resource:5', 6, 5, true, 2),
       (6, 'urn:topic-resource:6', 7, 6, true, 1),
       (7, 'urn:topic-resource:7', 8, 7, true, 1),
       (8, 'urn:topic-resource:8', 8, 8, true, 2),
       (9, 'urn:topic-resource:9', 1, 9, true, 1),
       (10, 'urn:topic-resource:10', 9, 10, true, 1);

insert into filter(id, public_id, subject_id, name)
values (1,'urn:filter:1', 1, 'F:1'),
       (2,'urn:filter:2', 1, 'F:2'),
       (3,'urn:filter:3', 1, 'F:3');

insert into relevance(id, public_id, name)
VALUES (1, 'urn:relevance:core', 'Core');

insert into resource_filter(id, public_id, resource_id, filter_id, relevance_id)
VALUES (1, 'urn:resource-filter:1', 1, 1, 1),
       (2, 'urn:resource-filter:2', 3, 1, 1),
       (3, 'urn:resource-filter:3', 5, 1, 1),
       (4, 'urn:resource-filter:4', 7, 1, 1),
       (5, 'urn:resource-filter:5', 9, 1, 1),
       (6, 'urn:resource-filter:6', 2, 2, 1),
       (7, 'urn:resource-filter:7', 4, 2, 1),
       (8, 'urn:resource-filter:8', 6, 2, 1),
       (9, 'urn:resource-filter:9', 8, 2, 1),
       (10, 'urn:resource-filter:10', 10, 2, 1);


