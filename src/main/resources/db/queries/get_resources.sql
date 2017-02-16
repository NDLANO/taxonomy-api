SELECT
  r.public_id                AS resource_public_id,
  coalesce(rtr.name, r.name) AS resource_name,
  r.content_uri              AS resource_content_uri
FROM
  resource r
  LEFT OUTER JOIN (SELECT *
                   FROM resource_translation
                   WHERE language_code = ?) rtr ON rtr.resource_id = r.id
WHERE 1 = 1
ORDER BY r.id;
