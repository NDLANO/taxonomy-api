SELECT *
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
WHERE e.public_id = ?