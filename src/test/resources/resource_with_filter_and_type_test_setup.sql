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
       (3, 'urn:resource:3', 'R:3', null);

insert into topic_resource(id, public_id, topic_id, resource_id, is_primary, rank)
VALUES (1, 'urn:topic-resource:1', 1, 1, true, 1),
       (2, 'urn:topic-resource:2', 1, 2, true, 2),
       (3, 'urn:topic-resource:3', 1, 3, true, 3);


insert into filter(id, public_id, subject_id, name) VALUES
        (1, 'urn:filter:1', 1, 'Vg1'),
        (2, 'urn:filter:2', 1, 'Vg2');

insert into relevance(id, public_id, name) VALUES
        (1, 'urn:relevance:core', 'Core');

insert into resource_filter(id, public_id, resource_id, filter_id, relevance_id) VALUES
        (1, 'urn:resource-filter:1', 1,1,1),
        (2, 'urn:resource-filter:2', 2,1,1),
        (3, 'urn:resource-filter:3', 3,2,1);

insert into resource_type (id, parent_id, public_id, name) VALUES
       (1, NULL, 'urn:resourcetype:video', 'Video');

insert into resource_resource_type(id, public_id, resource_id, resource_type_id) VALUES
       (1, 'urn:resource-resourcetype:1', 1, 1);

