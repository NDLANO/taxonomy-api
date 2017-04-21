SELECT
  f.public_id AS filter_public_id,
  coalesce(t.name, f.name) AS filter_name
FROM
  filter f
  LEFT OUTER JOIN
  (
    SELECT *
    FROM subject_translation
    WHERE language_code = ?
  ) t ON f.id = t.subject_id
WHERE
  1 = 1