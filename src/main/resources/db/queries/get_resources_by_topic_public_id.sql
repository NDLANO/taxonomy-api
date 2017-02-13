SELECT
  t.public_id   AS topic_id,
  r.name        AS resource_name,
  r.public_id   AS resource_id,
  r.content_uri AS resource_content_uri,
  rt.id         AS resource_type_id,
  rt.name       AS resource_type_name
FROM
  topic t
  INNER JOIN topic_resource tr ON tr.topic_id = t.id
  INNER JOIN resource r ON r.id = tr.resource_id
  LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
WHERE
  t.public_id = ?
  AND 1 = 1
ORDER BY r.id


