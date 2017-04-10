SELECT
  rt.public_id                 AS resource_type_public_id,
  coalesce(rttr.name, rt.name) AS resource_type_name,
  parent.public_id             AS resource_type_parent_public_id,
  rrt.public_id                AS resource_resource_type_public_id
FROM
  resource r
  INNER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  INNER JOIN resource_type rt ON rt.id = rrt.resource_type_id
  LEFT OUTER JOIN
  (
    SELECT *
    FROM resource_type_translation
    WHERE language_code = ?
  ) rttr ON rttr.resource_type_id = rt.id
  LEFT OUTER JOIN resource_type parent ON parent.id = rt.parent_id
WHERE r.public_id = ?
ORDER BY rt.id;
