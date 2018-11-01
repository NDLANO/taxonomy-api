with recursive topic_tree(topic_id, parent_topic_id, topic_rank, topic_level) as (
    -- topics first level
    select st.topic_id, 0 as parent_topic_id, st.rank as topic_rank, 0 as topic_level
    from subject_topic st
    where subject_id = (select id from subject where public_id = ?)

        union all

        -- subtopics level 2 and 3
        select ts.subtopic_id, ts.topic_id, ts."rank" as topic_rank, topic_level + 1
        from topic_subtopic ts
               inner join topic_tree on ts.topic_id = topic_tree.topic_id)
select topic_tree.*, t.public_id from topic_tree join topic t on topic_id = t.id
order by topic_level;