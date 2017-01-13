create table topic (
  id serial primary key,
  public_id varchar(255) not null,
  name varchar(255)
);

create unique index topic_public_id on topic (public_id);

