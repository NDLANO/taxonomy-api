SELECT
  ts.public_id AS connection_id,
  ts.is_primary,
  (SELECT public_id AS target_id FROM topic WHERE id = ts.subtopic_id),
  (SELECT path FROM cached_url WHERE public_id =
    (SELECT public_id FROM topic WHERE id = ts.subtopic_id))
FROM topic_subtopic ts
  JOIN topic t on ts.topic_id = t.id
WHERE 1 = 1;
