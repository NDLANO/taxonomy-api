WITH RECURSIVE tree (topic_id, public_id, name, parent_id, is_primary, level) AS (
  SELECT
    t.id,
    t.public_id,
    t.name,
    s.topic_id AS parent_id,
    FALSE      AS is_primary,
    0          AS level
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
  WHERE t.public_id = ?

  UNION ALL

  SELECT
    t.id,
    t.public_id,
    t.name,
    s.topic_id AS parent_id,
    s.is_primary,
    level + 1
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
    INNER JOIN tree tr ON tr.topic_id = s.topic_id
)
SELECT *
FROM tree;
