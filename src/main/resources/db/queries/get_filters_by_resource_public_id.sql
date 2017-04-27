SELECT
  f.name        AS filter_name,
  f.public_id   AS filter_public_id,
  rf.public_id  AS resource_filter_public_id,
  rel.public_id AS relevance_id
FROM
  resource r
  INNER JOIN resource_filter rf ON rf.resource_id = r.id
  INNER JOIN filter f ON rf.filter_id = f.id
  LEFT OUTER JOIN relevance rel ON rf.relevance_id = rel.id
WHERE r.public_id = ?