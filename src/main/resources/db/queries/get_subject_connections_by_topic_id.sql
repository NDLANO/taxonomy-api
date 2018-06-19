SELECT
  st.public_id AS connection_id,
  'parent-subject' as connection_type,
  st.is_primary,
  s.public_id  AS target_id,
  c."path"
FROM subject_topic st
  JOIN subject s ON st.subject_id = s.id
  JOIN topic t ON t.id = st.topic_id
  JOIN cached_url c ON c.public_id = s.public_id
WHERE
  t.public_id = ?;
