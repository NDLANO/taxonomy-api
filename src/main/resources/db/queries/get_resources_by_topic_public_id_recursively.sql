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
    parent.topic_id parent_id,
    s.is_primary,
    parent.level + 1
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
    INNER JOIN tree parent ON parent.topic_id = s.topic_id
)

SELECT
  r.public_id          resource_id,
  r.name               resource_name,
  t.public_id          topic_id,
  rrt.resource_type_id resource_type_id,
  rt.name              resource_type_name
FROM
  tree t
  INNER JOIN topic_resource tr ON tr.topic_id = t.topic_id
  INNER JOIN resource r ON r.id = tr.resource_id
  LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
WHERE 1 = 1
ORDER BY r.id
