SELECT
  f.name      AS filter_name,
  f.public_id AS filter_public_id
FROM filter f
INNER JOIN subject s on s.public_id = ?
WHERE f.subject_id = s.id
