-- create a test structure with subjects, topics, and resources as follows
-- (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)

-- S:1
--   - ST:1
--      - R:1
--      - R:2

insert into subject (id, public_id, name, content_uri)
VALUES (1, 'urn:subject:1', 'S:1', null);

insert into topic (id, public_id, name, content_uri, context)
VALUES (1, 'urn:topic:1', 'ST:1', null, false);

insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1);

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

insert into topic_resource (id, public_id, topic_id, resource_id, is_primary, rank)
VALUES (1, 'urn:topic-resource:1', 1, 1, true, 1),
       (2, 'urn:topic-resource:2', 1, 2, true, 2),
       (3, 'urn:topic-resource:3', 1, 3, true, 3),
       (4, 'urn:topic-resource:4', 1, 4, true, 4),
       (5, 'urn:topic-resource:5', 1, 5, true, 5),
       (6, 'urn:topic-resource:6', 1, 6, true, 6),
       (7, 'urn:topic-resource:7', 1, 7, true, 7),
       (8, 'urn:topic-resource:8', 1, 8, true, 8),
       (9, 'urn:topic-resource:9', 1, 9, true, 9),
       (10, 'urn:topic-resource:10', 1, 10, true, 10);


insert into filter (id, public_id, subject_id, name)
VALUES (1, 'urn:filter:1', 1, 'Year 1'),
       (2, 'urn:filter:2', 1, 'Year 2');

insert into relevance (id, public_id, name)
VALUES (1, 'urn:relevance:core', 'Core'),
       (2, 'urn:relevance:supplementary', 'Supplementary');

insert into resource_filter (id, public_id, resource_id, filter_id, relevance_id)
VALUES (1, 'urn:resource-filter:1', 1, 1, 1), -- R 1-5 is core in year 1
       (2, 'urn:resource-filter:2', 2, 1, 1),
       (3, 'urn:resource-filter:3', 3, 1, 1),
       (4, 'urn:resource-filter:4', 4, 1, 1),
       (5, 'urn:resource-filter:5', 5, 1, 1),

       (6, 'urn:resource-filter:6', 6, 2, 1), -- R 6-10 is core in year 2
       (7, 'urn:resource-filter:7', 7, 2, 1),
       (8, 'urn:resource-filter:8', 8, 2, 1),
       (9, 'urn:resource-filter:9', 9, 2, 1),
       (10, 'urn:resource-filter:10', 10, 2, 1),

       (11, 'urn:resource-filter:11', 1, 2, 2), -- R 1-5 is supplemental in year 2
       (12, 'urn:resource-filter:12', 2, 2, 2),
       (13, 'urn:resource-filter:13', 3, 2, 2),
       (14, 'urn:resource-filter:14', 4, 2, 2),
       (15, 'urn:resource-filter:15', 5, 2, 2);