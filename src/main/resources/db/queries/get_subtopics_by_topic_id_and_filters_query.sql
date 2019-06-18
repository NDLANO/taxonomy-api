SELECT s.public_id               AS subtopic_public_id,
       coalesce(tr.name, s.name) AS subtopic_name,
       s.content_uri             AS subtopic_content_uri,
       ts.is_primary             AS subtopic_is_primary,
       tf.filter_id
FROM topic t
       JOIN topic_subtopic ts ON ts.topic_id = t.id
       JOIN topic s ON ts.subtopic_id = s.id
       JOIN topic_filter tf ON tf.topic_id = s.id
       LEFT OUTER JOIN (SELECT * FROM topic_translation WHERE language_code = ?) tr ON s.id = tr.topic_id
WHERE t.public_id = ?
  AND tf.filter_id in
      (select id from filter where public_id in (?))