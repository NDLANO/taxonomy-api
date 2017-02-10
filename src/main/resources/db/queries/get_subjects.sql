SELECT
  s.public_id              subject_public_id,
  coalesce(t.name, s.name) subject_name,
  s.content_uri            subject_content_uri
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
