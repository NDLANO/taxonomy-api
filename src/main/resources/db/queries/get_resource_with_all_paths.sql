SELECT r.public_id                AS resource_public_id,
       coalesce(rtr.name, r.name) AS resource_name,
       r.content_uri              AS resource_content_uri,
       url.path                   AS resource_path,
       url.is_primary             as path_is_primary
FROM resource r
       LEFT OUTER JOIN (SELECT * FROM resource_translation WHERE language_code = ?) rtr ON rtr.resource_id = r.id
       LEFT OUTER JOIN (SELECT public_id, path, is_primary FROM cached_url) url ON r.public_id = url.public_id
WHERE r.public_id = ?
