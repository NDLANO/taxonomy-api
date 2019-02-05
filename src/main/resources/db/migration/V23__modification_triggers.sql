drop function if exists update_url_cache() cascade;

CREATE OR REPLACE FUNCTION update_url_cache()
  RETURNS trigger LANGUAGE plpgsql
AS
$function$
begin
  REFRESH MATERIALIZED VIEW CONCURRENTLY cached_url;
  RETURN NULL;
end;
$function$
;

create trigger refresh_paths
  after insert or update or delete or truncate
  on subject
execute procedure update_url_cache();

create trigger refresh_paths
  after insert or update or delete or truncate
  on topic
execute procedure update_url_cache();

create trigger refresh_paths
  after insert or update or delete or truncate
  on resource
execute procedure update_url_cache();

create trigger refresh_paths
  after insert or update or delete or truncate
  on subject_topic
execute procedure update_url_cache();

create trigger refresh_paths
  after insert or update or delete or truncate
  on topic_subtopic
execute procedure update_url_cache();

create trigger refresh_paths
after insert or update or delete or truncate
on topic_resource
execute procedure update_url_cache();



