SELECT
  e.public_id   AS public_id,
  e.content_uri AS content_uri,
  e.name        AS name,
  url.path      AS resource_path
FROM (
       SELECT
         id,
         public_id,
         content_uri,
         name
       FROM subject

       UNION ALL

       SELECT
         id,
         public_id,
         content_uri,
         name
       FROM topic

       UNION ALL

       SELECT
         id,
         public_id,
         content_uri,
         name
       FROM resource
     ) e
  INNER JOIN cached_url url ON url.public_id = e.public_id
WHERE e.public_id = ?
