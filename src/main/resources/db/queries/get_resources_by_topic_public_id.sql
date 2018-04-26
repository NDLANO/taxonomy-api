SELECT
  t.public_id                  AS topic_id,
  coalesce(rtr.name, r.name)   AS resource_name,
  r.public_id                  AS resource_public_id,
  r.content_uri                AS resource_content_uri,
  rt.id                        AS resource_type_id,
  rt.public_id                 AS resource_type_public_id,
  coalesce(rttr.name, rt.name) AS resource_type_name,
  tr.public_id                 AS connection_public_id,
  url.path                     AS resource_path,
  rel.public_id                AS relevance_public_id,
  tr.rank                      AS rank
FROM
  topic t
  INNER JOIN topic_resource tr ON tr.topic_id = t.id
  INNER JOIN resource r ON r.id = tr.resource_id
  LEFT OUTER JOIN resource_resource_type rrt ON rrt.resource_id = r.id
  LEFT OUTER JOIN resource_type rt ON rrt.resource_type_id = rt.id
  LEFT OUTER JOIN (SELECT *
                   FROM resource_translation
                   WHERE language_code = ?) rtr ON rtr.resource_id = r.id
  LEFT OUTER JOIN (SELECT *
                   FROM resource_type_translation
                   WHERE language_code = ?) rttr ON rttr.resource_type_id = rt.id
  LEFT OUTER JOIN resource_filter rf ON rf.resource_id = r.id
  LEFT OUTER JOIN filter f ON rf.filter_id = f.id
  LEFT OUTER JOIN relevance rel ON rf.relevance_id = rel.id
  LEFT OUTER JOIN cached_url_v url ON url.public_id = r.public_id
WHERE
  t.public_id = ?
  AND 1 = 1
  AND 2 = 2
ORDER BY tr.rank


WITH RECURSIVE tree (id, public_id, name, parent_public_id, level, is_primary, path) AS (
SELECT
contexts.id,
contexts.public_id,
contexts.name,
cast(NULL AS VARCHAR)         AS parent_public_id,
0                             AS level,
TRUE                          AS is_primary,
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
parent.public_id                       AS parent_public_id,
parent.level + 1                       AS level,
child.is_primary AND parent.is_primary AS is_primary,
parent.path || '/' || substr(child.public_id, 5)
FROM
(
SELECT
r.id,
r.public_id,
t.public_id   AS parent_public_id,
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
t.public_id    AS parent_public_id,
st.name,
tst.is_primary AS is_primary
FROM topic t
INNER JOIN topic_subtopic tst ON t.id = tst.topic_id
INNER JOIN topic st ON tst.subtopic_id = st.id

UNION ALL

SELECT
t.id,
t.public_id,
s.public_id   AS parent_public_id,
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
  is_primary
FROM
  tree t
WITH RECURSIVE tree (id, public_id, name, parent_public_id, level, is_primary, path) AS (
SELECT
contexts.id,
contexts.public_id,
contexts.name,
cast(NULL AS VARCHAR)         AS parent_public_id,
0                             AS level,
TRUE                          AS is_primary,
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
parent.public_id                       AS parent_public_id,
parent.level + 1                       AS level,
child.is_primary AND parent.is_primary AS is_primary,
parent.path || '/' || substr(child.public_id, 5)
FROM
(
SELECT
r.id,
r.public_id,
t.public_id   AS parent_public_id,
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
t.public_id    AS parent_public_id,
st.name,
tst.is_primary AS is_primary
FROM topic t
INNER JOIN topic_subtopic tst ON t.id = tst.topic_id
INNER JOIN topic st ON tst.subtopic_id = st.id

UNION ALL

SELECT
t.id,
t.public_id,
s.public_id   AS parent_public_id,
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
  is_primary
FROM
  tree t
WITH RECURSIVE tree (id, public_id, name, parent_public_id, level, is_primary, path) AS (
SELECT
contexts.id,
contexts.public_id,
contexts.name,
cast(NULL AS VARCHAR)         AS parent_public_id,
0                             AS level,
TRUE                          AS is_primary,
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
parent.public_id                       AS parent_public_id,
parent.level + 1                       AS level,
child.is_primary AND parent.is_primary AS is_primary,
parent.path || '/' || substr(child.public_id, 5)
FROM
(
SELECT
r.id,
r.public_id,
t.public_id   AS parent_public_id,
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
t.public_id    AS parent_public_id,
st.name,
tst.is_primary AS is_primary
FROM topic t
INNER JOIN topic_subtopic tst ON t.id = tst.topic_id
INNER JOIN topic st ON tst.subtopic_id = st.id

UNION ALL

SELECT
t.id,
t.public_id,
s.public_id   AS parent_public_id,
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
  is_primary
FROM
  tree t
