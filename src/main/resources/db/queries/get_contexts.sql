SELECT *
FROM (
  SELECT
    s.public_id                AS context_public_id,
    coalesce(rtr.name, s.name) AS context_name,
    s.content_uri              AS context_content_uri,
    url.path                   AS context_path
  FROM
    subject s
    LEFT OUTER JOIN
    (
      SELECT *
      FROM subject_translation
      WHERE language_code = ?
    ) rtr ON rtr.subject_id = s.id
    LEFT OUTER JOIN
    (
      SELECT *
      FROM cached_url
      WHERE is_primary = TRUE
    ) url ON url.public_id = s.public_id
  WHERE 1 = 1

  UNION ALL

  SELECT
    t.public_id                AS context_public_id,
    coalesce(rtr.name, t.name) AS context_name,
    t.content_uri              AS context_content_uri,
    url.path                   AS context_path
  FROM
    topic t
    LEFT OUTER JOIN
    (
      SELECT *
      FROM topic_translation
      WHERE language_code = ?
    ) rtr ON rtr.topic_id = t.id
    LEFT OUTER JOIN
    (
      SELECT *
      FROM cached_url
      WHERE is_primary = TRUE
    ) url ON url.public_id = t.public_id
  WHERE
    t.context = TRUE
    AND 2 = 2
) contexts
ORDER BY context_public_id;
