WITH RECURSIVE tree (id, public_id, name, content_uri, parent_id, parent_public_id, level) AS (
  SELECT
    t.id,
    t.public_id,
    coalesce(tr.name, t.name) name,
    t.content_uri,
    NULL                      parent_id,
    NULL                      parent_public_id,
    0                         level
  FROM
    subject s
    INNER JOIN subject_topic st ON s.id = st.subject_id
    INNER JOIN topic t ON t.id = st.topic_id
    LEFT OUTER JOIN (SELECT *
                     FROM topic_translation
                     WHERE language_code = ?) tr ON t.id = tr.topic_id
  WHERE s.public_id = ?

  UNION ALL

  SELECT
    t.id,
    t.public_id,
    coalesce(tr.name, t.name) name,
    t.content_uri,
    parent.id                 parent_id,
    parent.public_id          parent_public_id,
    parent.level + 1
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic s ON t.id = s.subtopic_id
    LEFT OUTER JOIN (SELECT *
                     FROM topic_translation
                     WHERE language_code = ?) tr ON t.id = tr.topic_id
    INNER JOIN tree parent ON parent.id = s.topic_id
)

SELECT
  t.public_id,
  t.name,
  t.content_uri,
  t.parent_public_id,
  t.level
FROM
  tree t
WHERE 1 = 1
ORDER BY t.level
