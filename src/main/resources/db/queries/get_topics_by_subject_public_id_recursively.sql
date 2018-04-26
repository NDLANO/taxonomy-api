WITH RECURSIVE tree (id, public_id, name, content_uri, parent_id, parent_public_id, connection_public_id, level, rank) AS (
  SELECT
    t.id,
    t.public_id,
    t.name,
    t.content_uri,
    cast(NULL AS INT) AS parent_id,
    s.public_id       AS parent_public_id,
    st.public_id      AS connection_public_id,
    0                 AS level,
    st.rank           AS rank
  FROM
    subject s
    INNER JOIN subject_topic st ON s.id = st.subject_id
    INNER JOIN topic t ON t.id = st.topic_id
  WHERE s.public_id = ?

  UNION ALL

  SELECT
    t.id,
    t.public_id,
    t.name           AS NAME,
    t.content_uri,
    parent.id        AS parent_id,
    parent.public_id AS parent_public_id,
    tst.public_id    AS connection_public_id,
    parent.level + 1,
    tst.rank         AS rank
  FROM
    topic t
    LEFT OUTER JOIN topic_subtopic tst ON t.id = tst.subtopic_id
    INNER JOIN tree parent ON parent.id = tst.topic_id
)

SELECT DISTINCT
  t.public_id,
  coalesce(tr.name, t.name) AS name,
  t.content_uri,
  t.parent_public_id,
  t.level,
  t.connection_public_id,
  resf.public_id            AS resource_filter_public_id,
  tf.public_id              AS topic_filter_public_id,
  url.path                  AS topic_path,
  rel.public_id             AS relevance_public_id,
  f.name                    AS filter_name,
  f.public_id               AS filter_public_id,
  t.rank
FROM
  tree t
  LEFT OUTER JOIN (SELECT *
                   FROM topic_translation
                   WHERE language_code = ?) tr ON t.id = tr.topic_id
  LEFT OUTER JOIN topic_filter tf ON tf.topic_id = t.id
  LEFT OUTER JOIN filter f ON tf.filter_id = f.id
  LEFT OUTER JOIN relevance rel ON tf.relevance_id = rel.id

  LEFT OUTER JOIN topic_resource tore ON tore.topic_id = t.id
  LEFT OUTER JOIN resource_filter rf ON tore.resource_id = rf.resource_id
  LEFT OUTER JOIN filter resf ON rf.filter_id = resf.id

  LEFT OUTER JOIN cached_url_v url ON url.public_id = t.public_id
WHERE
  1 = 1
ORDER BY t.level, t.rank

