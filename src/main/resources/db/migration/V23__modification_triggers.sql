CREATE TABLE freshness (
  resource VARCHAR(128) NOT NULL,
  last_modified TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX freshness_idx
  ON freshness (resource);

drop function if exists updatelastmodified() cascade;

CREATE OR REPLACE FUNCTION updatelastmodified()
  RETURNS trigger LANGUAGE plpgsql
AS
$function$
begin
  insert into public.freshness (resource, last_modified)
  values (TG_TABLE_NAME, now())
  on conflict (resource) do update set last_modified = now();
  return NEW;
end;
$function$
;

create trigger update_freshness
  after insert or update or delete or truncate
  on filter
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on filter_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on relevance
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on relevance_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource_filter
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource_resource_type
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource_type
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on resource_type_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on subject
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on subject_topic
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on subject_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on topic
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on topic_filter
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on topic_resource
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on topic_subtopic
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on topic_translation
execute procedure updatelastmodified();

create trigger update_freshness
  after insert or update or delete or truncate
  on url_map
execute procedure updatelastmodified();

