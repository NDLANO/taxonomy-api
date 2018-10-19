with recursive topic_tree(topic_id, parent_topic_id, topic_rank, topic_level) as (
    -- topics first level
    select ts_one.topic_id, 0 as parent_topic_id, ts_one.rank as topic_rank, 0 as topic_level
    from topic_subtopic ts_one
    where ts_one.topic_id = (select id from topic t where t.public_id = ?)

        union all

        -- subtopics level 2 and 3
        select ts_next.subtopic_id, ts_next.topic_id, ts_next.rank as topic_rank, topic_level + 1
        from topic_subtopic ts_next
               inner join topic_tree on ts_next.topic_id = topic_tree.topic_id)
select topic_tree.*, t.public_id from topic_tree join topic t on topic_id = t.id
order by topic_level;