DROP view cached_url;

CREATE MATERIALIZED VIEW cached_url as (
  WITH RECURSIVE tree (id, public_id, name, parent_public_id, level, is_primary, path) AS (
  SELECT
  contexts.id,
  contexts.public_id,
  contexts.name,
  cast(NULL AS VARCHAR) AS parent_public_id,
  0 AS level,
  TRUE AS is_primary,
  '/' || substr(contexts.public_id, 5) AS path
  FROM
(
  SELECT
  id,
  public_id,
  name
  FROM subject

  UNION ALL

  SELECT
  id,
  public_id,
  name
  FROM topic
  WHERE context = TRUE
) contexts

  UNION ALL

  SELECT
  child.id,
  child.public_id,
  child.name,
  parent.public_id AS parent_public_id,
  parent.level + 1 AS level,
  child.is_primary AND parent.is_primary AS is_primary,
  parent.path || '/' || substr(child.public_id, 5)
  FROM
(
  SELECT
  r.id,
  r.public_id,
  t.public_id AS parent_public_id,
  r.name,
  tr.is_primary AS is_primary
  FROM
  resource r
  INNER JOIN topic_resource tr ON tr.resource_id = r.id
  INNER JOIN topic t ON tr.topic_id = t.id

  UNION ALL

  SELECT
  st.id,
  st.public_id,
  t.public_id AS parent_public_id,
  st.name,
  tst.is_primary AS is_primary
  FROM topic t
  INNER JOIN topic_subtopic tst ON t.id = tst.topic_id
  INNER JOIN topic st ON tst.subtopic_id = st.id

  UNION ALL

  SELECT
  t.id,
  t.public_id,
  s.public_id AS parent_public_id,
  t.name,
  st.is_primary AS is_primary
  FROM
  subject s
  INNER JOIN subject_topic st ON s.id = st.subject_id
  INNER JOIN topic t ON st.topic_id = t.id
) child
  INNER JOIN tree AS parent ON parent.public_id = child.parent_public_id
  WHERE 1 = 1
)

  SELECT
  id,
  public_id,
  path,
  is_primary,
  parent_public_id
  FROM
  tree t
);

CREATE UNIQUE INDEX cached_url_path_idx
  ON cached_url (path);
