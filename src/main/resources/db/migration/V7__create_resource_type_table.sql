CREATE TABLE resource_type (
  id        SERIAL PRIMARY KEY,
  parent_id int references resource_type(id),
  public_id VARCHAR(255) NOT NULL,
  name      VARCHAR(255)
);

CREATE UNIQUE INDEX resource_type_public_id
  ON resource_type (public_id);

