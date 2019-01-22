-- Creates subtopics with different filters
--
-- Subjects       S:1   S:2
--                   \  /
--                    \/
-- Parent topic       T1 (has filter F:1 and F:2)
--                     |
-- Subtopics     T1-1, T1-2, T1-3 (have filter F:1),
--               T1-4, T1-5, T1-6, T1-7 (have filter F:2)

insert into subject (id, public_id, name, content_uri)
VALUES (1, 'urn:subject:1', 'S:1', null),
       (2, 'urn:subject:2', 'S:2', null);

insert into topic (id, public_id, name, content_uri, context)
VALUES (1, 'urn:topic:1', 'T1', null, false),
       (2, 'urn:topic:2', 'T1-1', null, false),
       (3, 'urn:topic:3', 'T1-2', null, false),
       (4, 'urn:topic:4', 'T1-3', null, false),
       (5, 'urn:topic:5', 'T1-4', null, false),
       (6, 'urn:topic:6', 'T1-5', null, false),
       (7, 'urn:topic:7', 'T1-6', null, false),
       (8, 'urn:topic:8', 'T1-7', null, false);

insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1),
       (2, 'urn:subject-topic:2', 1, 2, false, 1);

insert into topic_subtopic (id, public_id, topic_id, subtopic_id, is_primary, rank)
VALUES (1, 'urn:topic-subtopic:1', 1, 2, true, 1),
       (2, 'urn:topic-subtopic:2', 1, 3, true, 2),
       (3, 'urn:topic-subtopic:3', 1, 4, true, 3),
       (4, 'urn:topic-subtopic:4', 1, 5, true, 4),
       (5, 'urn:topic-subtopic:5', 1, 6, true, 5),
       (6, 'urn:topic-subtopic:6', 1, 7, true, 6),
       (7, 'urn:topic-subtopic:7', 1, 8, true, 7);

insert into filter (id, public_id, subject_id, name)
VALUES (1, 'urn:filter:1', 1, 'F:1'),
       (2, 'urn:filter:2', 2, 'F:2');

insert into relevance (id, public_id, name)
VALUES (1, 'urn:relevance:core', 'Kjernestoff');

insert into topic_filter (id, public_id, topic_id, filter_id, relevance_id)
VALUES (1, 'urn:topic-filter:1', 1, 1, 1),
       (2, 'urn:topic-filter:2', 1, 2, 1),
       (3, 'urn:topic-filter:3', 2, 1, 1),
       (4, 'urn:topic-filter:4', 3, 1, 1),
       (5, 'urn:topic-filter:5', 4, 1, 1),
       (6, 'urn:topic-filter:6', 5, 2, 1),
       (7, 'urn:topic-filter:7', 6, 2, 1),
       (8, 'urn:topic-filter:8', 7, 2, 1),
       (9, 'urn:topic-filter:9', 8, 2, 1);
