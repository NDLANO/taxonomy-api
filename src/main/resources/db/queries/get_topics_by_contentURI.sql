SELECT
  t.public_id                  AS topic_public_id,
  coalesce(ttr.name, t.name)   AS topic_name,
  t.content_uri                AS topic_content_uri,
  url.path                     AS topic_path
FROM
  topic t
  LEFT OUTER JOIN
  (
    SELECT *
    FROM cached_url_v
    WHERE is_primary = TRUE
  ) url ON url.public_id = t.public_id
  LEFT OUTER JOIN (SELECT *
                   FROM topic_translation
                   WHERE language_code = ?) ttr ON ttr.topic_id = t.id
WHERE 1 = 1
ORDER BY t.id;
