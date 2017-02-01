WITH RECURSIVE tree (topic_id, public_id, name, parent_id, is_primary, level) AS (
  SELECT
    t.id        AS topic_id,
    t.public_id AS public_id,
    t.name      AS name,
    0           AS parent_id,
    FALSE       AS is_primary,
    0           AS level
  FROM
    subject s
    INNER JOIN subject_topic st ON st.subject_id = s.id
    INNER JOIN topic t ON st.topic_id = t.id
  WHERE
    s.public_id = ?

  UNION ALL

  SELECT
    t.id             AS topic_id,
    t.public_id      AS public_id,
    t.name           AS name,
    parent.topic_id  AS parent_id,
    st.is_primary    AS is_primary,
    parent.level + 1 AS level
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic st ON t.id = st.subtopic_id
    INNER JOIN tree parent ON parent.topic_id = st.topic_id
)

SELECT
  r.public_id  AS resource_public_id,
  r.name       AS resource_name,
  t.public_id  AS topic_public_id,
  rt.public_id AS resource_type_public_id,
  rt.name      AS resource_type_name
FROM
  tree t
  INNER JOIN topic_resource tr ON tr.topic_id = t.topic_id
  INNER JOIN resource r ON r.id = tr.resource_id
  LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
WHERE 1 = 1
ORDER BY r.id;
