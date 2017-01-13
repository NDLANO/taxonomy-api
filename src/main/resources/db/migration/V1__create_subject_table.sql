create table subject (
  id serial primary key,
  public_id varchar(255) not null,
  name varchar(255)
);

create unique index subject_public_id on subject (public_id);

