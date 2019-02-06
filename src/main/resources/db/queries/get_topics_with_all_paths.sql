SELECT t.public_id               AS topic_public_id,
       coalesce(tr.name, t.name) AS topic_name,
       t.content_uri             AS topic_content_uri,
       url.path                  AS topic_path,
       url.is_primary            AS path_is_primary
FROM topic t
       LEFT OUTER JOIN (SELECT * FROM topic_translation WHERE language_code = ?) tr ON t.id = tr.topic_id
       LEFT OUTER JOIN (SELECT * FROM cached_url) url ON t.public_id = url.public_id
WHERE t.public_id = ?