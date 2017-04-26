SELECT
  f.name        AS filter_name,
  f.public_id   AS filter_public_id,
  rf.public_id  AS filter_resource_public_id,
  rel.public_id AS relevance_id
FROM filter f
  INNER JOIN resource r ON r.public_id = ?
  INNER JOIN resource_filter rf ON rf.resource_id = r.id
  INNER JOIN relevance rel ON rf.relevance_id = rel.id
WHERE f.id = rf.filter_id