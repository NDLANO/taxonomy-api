SELECT
  r.public_id              AS relevance_public_id,
  coalesce(t.name, r.name) AS relevance_name
FROM
  relevance r
  LEFT OUTER JOIN
  (
    SELECT *
    FROM relevance_translation
    WHERE language_code = ?
  ) t ON r.id = t.relevance_id
WHERE
  1 = 1
