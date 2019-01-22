SELECT r.public_id                  AS resource_public_id,
       coalesce(rtr.name, r.name)   AS resource_name,
       r.content_uri                AS resource_content_uri,
       url.path                     AS resource_path,
       url.is_primary               as path_is_primary,
       rt.id                        AS resource_type_id,
       rt.public_id                 AS resource_type_public_id,
       coalesce(rttr.name, rt.name) AS resource_type_name
FROM resource r
            LEFT OUTER JOIN (SELECT * FROM cached_url) url ON url.public_id = r.public_id
            LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
            LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
            LEFT OUTER JOIN (SELECT * FROM resource_translation WHERE language_code = ?) rtr ON rtr.resource_id = r.id
            LEFT OUTER JOIN (SELECT * FROM resource_type_translation WHERE language_code = ?) rttr
         ON rttr.resource_type_id = rt.id
WHERE r.content_uri = ?
ORDER BY r.id;