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
    parent.topic_id AS parent_id,
    s.is_primary,
    parent.level + 1
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
    INNER JOIN tree parent ON parent.topic_id = s.topic_id
)

SELECT
  t.public_id                  AS topic_id,
  coalesce(rtr.name, r.name)   AS resource_name,
  r.public_id                  AS resource_public_id,
  r.content_uri                AS resource_content_uri,
  rt.id                        AS resource_type_id,
  rt.public_id                 AS resource_type_public_id,
  coalesce(rttr.name, rt.name) AS resource_type_name,
  url.path                     AS resource_path
FROM
  tree t
  INNER JOIN topic_resource tr ON tr.topic_id = t.topic_id
  INNER JOIN resource r ON r.id = tr.resource_id
  LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
  LEFT OUTER JOIN (SELECT *
                   FROM resource_translation
                   WHERE language_code = ?) rtr ON rtr.resource_id = r.id
  LEFT OUTER JOIN (SELECT *
                   FROM resource_type_translation
                   WHERE language_code = ?) rttr ON rttr.resource_type_id = rt.id
  LEFT OUTER JOIN cached_url url ON url.public_id = r.public_id
WHERE
  1 = 1
ORDER BY r.id
