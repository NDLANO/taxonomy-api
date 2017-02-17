WITH RECURSIVE tree (id, public_id, parent_id, name, level) AS (
  SELECT
    rt.id        AS id,
    rt.public_id AS public_id,
    rt.parent_id AS parent_id,
    rt.name      AS name,
    0            AS level
  FROM
    resource_type rt
  WHERE
    1 = 1

  UNION ALL

  SELECT
    rt.id            AS id,
    rt.public_id     AS public_id,
    rt.parent_id     AS parent_id,
    rt.name          AS name,
    parent.level + 1 AS level
  FROM
    resource_type rt
    INNER JOIN tree parent ON parent.id = rt.parent_id
)

SELECT
  t.id,
  t.public_id,
  t.parent_id,
  coalesce(rttr.name, t.name) AS name,
  t.level
FROM
  tree t
  LEFT OUTER JOIN (SELECT *
                   FROM resource_type_translation
                   WHERE language_code = ?) rttr ON rttr.resource_type_id = t.id
WHERE
  2 = 2
ORDER BY t.level, t.id;
