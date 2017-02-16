SELECT
  rt.public_id                 AS resource_type_public_id,
  coalesce(rttr.name, rt.name) AS resource_type_name
FROM
  resource_type rt
  LEFT OUTER JOIN (SELECT *
                   FROM resource_type_translation
                   WHERE language_code = ?) rttr ON rttr.resource_type_id = rt.id
WHERE 1 = 1
ORDER BY rt.id;
