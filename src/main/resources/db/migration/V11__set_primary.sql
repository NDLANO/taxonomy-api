UPDATE subject_topic
SET is_primary = TRUE
WHERE id IN (
  SELECT min(id) AS id
  FROM subject_topic
  WHERE topic_id NOT IN (SELECT topic_id
                         FROM subject_topic
                         WHERE is_primary = TRUE)
  GROUP BY topic_id
);

UPDATE topic_subtopic
SET is_primary = TRUE
WHERE id IN (
  SELECT min(id) AS id
  FROM topic_subtopic
  WHERE subtopic_id NOT IN (SELECT subtopic_id
                            FROM topic_subtopic
                            WHERE is_primary = TRUE)
  GROUP BY subtopic_id
);

UPDATE topic_resource
SET is_primary = TRUE
WHERE id IN (
  SELECT min(id) AS id
  FROM topic_resource
  WHERE resource_id NOT IN (SELECT resource_id
                            FROM topic_resource
                            WHERE is_primary = TRUE)
  GROUP BY resource_id
);
