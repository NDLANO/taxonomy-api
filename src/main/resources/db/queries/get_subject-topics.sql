select st.public_id as subject_topic_id,
       t.public_id  as topic_id,
       s.public_id  as subject_id,
       st.is_primary,
       st."rank"
from subject_topic st
         join topic t on st.topic_id = t.id
         join subject s on st.subject_id = s.id;