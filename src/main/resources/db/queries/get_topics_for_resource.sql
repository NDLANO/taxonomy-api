SELECT
  r.public_id               AS resource_id,
  t.public_id               AS id,
  coalesce(trl.name, t.name) AS name,
  t.content_uri             AS content_uri,
  tr.is_primary             AS is_primary,
  tr.public_id              AS connection_id
FROM
  topic t
  LEFT OUTER JOIN
  (
    SELECT name, topic_id
    FROM topic_translation
    WHERE language_code = ?
  ) trl ON t.id = trl.topic_id
  INNER JOIN topic_resource tr
    ON t.id = tr.topic_id
  INNER JOIN resource r
    ON tr.resource_id = r.id

WHERE
  r.public_id = ?