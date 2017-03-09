WITH RECURSIVE tree (id, public_id, name, content_uri, parent_id, parent_public_id, level) AS (
  SELECT
    t.id,
    t.public_id,
    t.name,
    t.content_uri,
    cast(NULL AS INT)     AS parent_id,
    cast(NULL AS VARCHAR) AS parent_public_id,
    0                     AS level
  FROM
    subject s
    INNER JOIN subject_topic st ON s.id = st.subject_id
    INNER JOIN topic t ON t.id = st.topic_id
  WHERE s.public_id = ?

  UNION ALL

  SELECT
    t.id,
    t.public_id,
    t.name                AS name,
    t.content_uri,
    parent.id                parent_id,
    parent.public_id         parent_public_id,
    parent.level + 1
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
    INNER JOIN tree parent ON parent.id = s.topic_id
)

SELECT
  t.public_id,
  coalesce(tr.name, t.name) AS name,
  t.content_uri,
  t.parent_public_id,
  t.level,
  url.path                  AS topic_path
FROM
  tree t
  LEFT OUTER JOIN (SELECT *
                   FROM topic_translation
                   WHERE language_code = ?) tr ON t.id = tr.topic_id
  LEFT OUTER JOIN cached_url url ON url.public_id = t.public_id
WHERE 1 = 1
ORDER BY t.level
