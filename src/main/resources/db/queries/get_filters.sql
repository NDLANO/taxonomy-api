SELECT
  f.public_id              AS filter_public_id,
  coalesce(t.name, f.name) AS filter_name,
  s.public_id              AS subject_id
FROM
  filter f
  LEFT OUTER JOIN
  (
    SELECT *
    FROM filter_translation
    WHERE language_code = ?
  ) t ON f.id = t.filter_id

  LEFT OUTER JOIN subject s ON f.subject_id = s.id

WHERE
  1 = 1