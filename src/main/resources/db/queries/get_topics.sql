SELECT
  t.public_id               AS topic_public_id,
  coalesce(tr.name, t.name) AS topic_name,
  t.content_uri             AS topic_content_uri
FROM
  topic t
  LEFT OUTER JOIN
  (
    SELECT *
    FROM topic_translation
    WHERE language_code = ?
  ) tr ON t.id = tr.topic_id
WHERE
  1 = 1