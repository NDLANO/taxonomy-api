select tr.public_id as topic_resource_id,
       t.public_id  as topic_id,
       r.public_id  as resource_id,
       tr.is_primary,
       tr."rank"
from topic_resource tr
         join topic t on tr.topic_id = t.id
         join resource r on tr.resource_id = r.id