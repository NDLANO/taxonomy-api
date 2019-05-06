select tf.public_id as topic_resource_id, t.public_id as topic_id, f.public_id as filter_id, r.public_id as relevance_id
from topic_filter tf
         join topic t on tf.topic_id = t.id
         join filter f on tf.filter_id = f.id
         join relevance r on tf.relevance_id = r.id;