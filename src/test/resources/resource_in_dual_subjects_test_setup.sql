-- create a test structure with subjects, topics, and resources as follows
-- (S=subject, ST = subject-topic, TST = topic-subtopic, R = resource, F = filter)

-- S:1
--   - ST:1
--      - R:1

-- S:2
--   - ST:2
--      - R:1


insert into subject (id, public_id, name, content_uri)
VALUES (1, 'urn:subject:1', 'S:1', null),
       (2, 'urn:subject:2', 'S:2', null);

insert into topic (id, public_id, name, content_uri, context)
VALUES (1, 'urn:topic:1', 'ST:1', null, false),
       (2, 'urn:topic:2', 'ST:2', null, false);


insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank)
VALUES (1, 'urn:subject-topic:1', 1, 1, true, 1),
       (2, 'urn:subject-topic:2', 2, 2, true, 1);

insert into resource (id, public_id, name, content_uri)
VALUES (1, 'urn:resource:1', 'R:1', null);

insert into topic_resource(id, public_id, topic_id, resource_id, is_primary, rank)
VALUES (1, 'urn:topic-resource:1', 1, 1, true, 1),
       (2, 'urn:topic-resource:2', 2, 1, false, 1);


