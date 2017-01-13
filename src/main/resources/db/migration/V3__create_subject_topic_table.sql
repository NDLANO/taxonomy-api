create table subject_topic (
  id serial primary key,
  public_id varchar(255) not null,
  topic_id int references topic(id),
  subject_id int references subject(id),
  is_primary boolean
);

create unique index subject_topic_public_id on subject_topic (public_id);

