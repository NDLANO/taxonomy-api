with recursive topic_tree(topic_id, level) as (
    -- topics first level
    select st.topic_id, 0 as level
    from subject_topic st
    where subject_id =
          (select id from subject where public_id = 'urn:subject:1')

        union all

        -- subtopics level 2 and 3
        select ts.subtopic_id, level + 1
        from topic_subtopic ts
               inner join topic_tree on ts.topic_id = topic_tree.topic_id)
select topic_tree.*, r.public_id, r.name, tr.rank as rank_in_parent
from topic_tree
       join topic_resource tr on topic_tree.topic_id = tr.topic_id
       join resource r on tr.resource_id = r.id;


