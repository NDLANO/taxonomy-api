WITH RECURSIVE tree (id, public_id, name, child_public_id, level, is_primary) AS (
  SELECT
    e.id,
    e.public_id,
    e.name,
    cast(NULL AS VARCHAR) AS child_public_id,
    0                     AS level,
    TRUE                  AS is_primary
  FROM (
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

         UNION ALL

         SELECT
           id,
           public_id,
           name
         FROM resource
       ) e
  WHERE e.public_id = ?

  UNION ALL

  SELECT
    parent.id,
    parent.public_id,
    parent.name,
    child.public_id AS child_public_id,
    child.level + 1 AS level,
    parent.is_primary
  FROM
    (
      SELECT
        t.id,
        t.public_id,
        r.public_id   AS child_public_id,
        t.name,
        tr.is_primary AS is_primary
      FROM
        resource r
        INNER JOIN topic_resource tr ON tr.resource_id = r.id
        INNER JOIN topic t ON tr.topic_id = t.id

      UNION ALL

      SELECT
        t.id,
        t.public_id,
        st.public_id   AS child_public_id,
        t.name,
        tst.is_primary AS is_primary
      FROM topic t
        INNER JOIN topic_subtopic tst ON t.id = tst.topic_id
        INNER JOIN topic st ON tst.subtopic_id = st.id

      UNION ALL

      SELECT
        s.id,
        s.public_id,
        t.public_id   AS child_public_id,
        s.name,
        st.is_primary AS is_primary
      FROM
        subject s
        INNER JOIN subject_topic st ON s.id = st.subject_id
        INNER JOIN topic t ON st.topic_id = t.id
    ) parent
    INNER JOIN tree AS child ON child.public_id = parent.child_public_id
  WHERE 1 = 1
)

SELECT *
FROM
  tree t
ORDER BY level DESC