SELECT
  f.name        AS filter_name,
  f.public_id   AS filter_public_id,
  tf.public_id  AS topic_filter_public_id,
  rel.public_id AS relevance_id
FROM
  topic t
  INNER JOIN topic_filter tf ON t.id = tf.topic_id
  INNER JOIN filter f ON tf.filter_id = f.id
  LEFT OUTER JOIN relevance rel ON tf.relevance_id = rel.id
WHERE t.public_id = ?