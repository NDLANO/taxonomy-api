SELECT
    t.public_id topic_id,
    r.name resource_name,
    r.public_id resource_id
  FROM
    topic t
    INNER JOIN topic_resource tr ON tr.topic_id = t.id
    INNER JOIN resource r ON r.id = tr.resource_id
  WHERE
    t.public_id = ?


