SELECT
  s.public_id               AS subtopic_public_id,
  coalesce(tr.name, s.name) AS subtopic_name,
  s.content_uri             AS subtopic_content_uri,
  ts.is_primary             AS subtopic_is_primary
FROM
  topic t
  INNER JOIN topic_subtopic ts
    ON ts.topic_id = t.id
  INNER JOIN topic s
    ON ts.subtopic_id = s.id
  LEFT OUTER JOIN
  (
    SELECT *
    FROM topic_translation
    WHERE language_code = ?
  ) tr ON s.id = tr.topic_id
WHERE
  1 = 1