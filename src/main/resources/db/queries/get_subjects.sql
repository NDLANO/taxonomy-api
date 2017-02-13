SELECT
  s.public_id              AS subject_public_id,
  coalesce(t.name, s.name) AS subject_name,
  s.content_uri            AS subject_content_uri
FROM
  subject s
  LEFT OUTER JOIN
  (
    SELECT *
    FROM subject_translation
    WHERE language_code = ?
  ) t ON s.id = t.subject_id
WHERE
  1 = 1
