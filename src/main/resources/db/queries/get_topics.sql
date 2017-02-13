SELECT
  t.public_id               topic_public_id,
  coalesce(tr.name, t.name) topic_name,
  t.content_uri             topic_content_uri
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