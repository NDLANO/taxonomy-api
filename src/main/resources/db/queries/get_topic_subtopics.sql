select ts.public_id as topic_subtopic_id,
       t.public_id  as topic_id,
       st.public_id as subtopic_id,
       ts.is_primary,
       ts."rank"
from topic_subtopic ts
         join topic t on ts.topic_id = t.id
         join topic st on ts.subtopic_id = st.id;