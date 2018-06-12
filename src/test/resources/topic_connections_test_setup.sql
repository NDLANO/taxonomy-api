-- creates a structure where topic 2 is shared (one parent topic, one parent subject) and has 2 subtopics

insert into subject (id, public_id, name, content_uri) VALUES (1, 'urn:subject:1000', 'S1', null);
insert into subject (id, public_id, name, content_uri) VALUES (2, 'urn:subject:2000', 'S2', null);

insert into topic (id, public_id, name, content_uri, context) VALUES (1, 'urn:topic:1000', 'T1', null, false);
insert into topic (id, public_id, name, content_uri, context) VALUES (2, 'urn:topic:2000', 'T2', null, false);
insert into topic (id, public_id, name, content_uri, context) VALUES (3, 'urn:topic:3000', 'T3', null, false);
insert into topic (id, public_id, name, content_uri, context) VALUES (4, 'urn:topic:4000', 'T4', null, false);

insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank) VALUES (1, 'urn:subject-topic:1000', 1, 1, true, 1);
insert into subject_topic (id, public_id, topic_id, subject_id, is_primary, rank) VALUES (2, 'urn:subject-topic:2000', 2, 2, false, 1);

insert into topic_subtopic(id, public_id, topic_id, subtopic_id, is_primary, rank) VALUES (1, 'urn:topic-subtopic:1000', 1, 2, true, 1);
insert into topic_subtopic(id, public_id, topic_id, subtopic_id, is_primary, rank) VALUES (2, 'urn:topic-subtopic:2000', 2, 3, true, 1);
insert into topic_subtopic(id, public_id, topic_id, subtopic_id, is_primary, rank) VALUES (3, 'urn:topic-subtopic:3000', 2, 4, false , 2);
