-- create a test structure with subjects, topics and subtopics as follows
-- (S=subject, ST = subject-topic, TST = topic-subtopic)

-- S:1
--   - ST:1
--        - TST: 1-1
--   - ST:2
--        - TST:2-1
--   - ST:3
--        - TST:3-1
--        - TST:3-2
--        - TST:3-3

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
       (8, 'urn:topic:8', 'TST:3-3', null, false);

insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1),
       (2, 'urn:subject-topic:2', 3, 1, true, 2),
       (3, 'urn:subject-topic-3', 5, 1, true, 3);

insert into topic_subtopic (id, public_id, topic_id, subtopic_id, is_primary, rank)
VALUES (1, 'urn:topic-subtopic:1', 1, 2, true, 1),
       (2, 'urn:topic-subtopic:2', 3, 4, true, 1),
       (3, 'urn:topic-subtopic:3', 5, 6, true, 1),
       (4, 'urn:topic-subtopic:4', 5, 7, true, 2),
       (5, 'urn:topic-subtopic:5', 5, 8, true, 3);

