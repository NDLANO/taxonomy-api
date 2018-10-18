with recursive topic_tree(topic_id, level) as (
    select st.topic_id, 0 as level
    from subject_topic st
    where subject_id = (select id from subject s where s.public_id = ?)

    union all

  select ts.subtopic_id, level + 1 from topic_subtopic ts
    inner join topic_tree on ts.topic_id = topic_tree.topic_id)

select topic_tree.topic_id,
  topic_tree.level,
  r.public_id    AS resource_public_id,
  r.name         AS resource_name,
  r.content_uri  AS resource_content_uri,
  t.public_id    AS topic_public_id,
  rt.public_id   AS resource_type_public_id,
  rt.name        AS resource_type_name,
  tr.public_id   AS connection_public_id,
  f.public_id    AS filter_public_id,
  rf.public_id   AS resource_filter_public_id,
  rel.public_id  AS relevance_public_id,
  url.path       AS resource_path,
  tr.rank        AS rank
from topic_tree
  join topic_resource tr on topic_tree.topic_id = tr.topic_id
  join topic t on topic_tree.topic_id = t.id
  left outer join resource r on tr.resource_id = r.id
  left outer join resource_resource_type rrt on r.id = rrt.resource_id
  left outer join resource_type rt on rrt.resource_type_id = rt.id
  left outer join resource_filter rf on r.id = rf.resource_id
  left outer join relevance rel on rf.relevance_id = rel.id
  left outer join filter f on f.id = rf.filter_id
  join cached_url url on url.public_id = r.public_id
where
  1 = 1
  AND
  2 = 2;
