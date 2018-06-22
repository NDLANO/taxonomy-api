select
  ts.public_id as connection_id,
  'subtopic'   as connection_type,
  ts.is_primary,
  (select public_id as target_id
   from topic
   where id = ts.subtopic_id),
  c."path"
from topic_subtopic ts
  join topic t on ts.topic_id = t.id
  join cached_url c on c.public_id = (select public_id as subtopic_public_id
                                      from topic
                                      where id = ts.subtopic_id)
where t.public_id = ?
      and c.parent_public_id = t.public_id;
