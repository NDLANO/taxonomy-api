select
  ts.public_id as connection_id,
  'parent-topic' as connection_type,
  ts.is_primary,
  parent.public_id as target_id,
  c."path"
from topic_subtopic ts
  join topic t on ts.subtopic_id = t.id
  join topic parent on ts.topic_id = parent.id
  join cached_url c on c.public_id = parent.public_id
where t.public_id = ?
