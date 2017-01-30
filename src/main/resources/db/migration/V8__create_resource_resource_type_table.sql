CREATE TABLE resource_resource_type (
  id               SERIAL PRIMARY KEY,
  public_id        VARCHAR(255) NOT NULL,
  resource_id      INT REFERENCES resource (id),
  resource_type_id INT REFERENCES resource_type (id)
);

CREATE UNIQUE INDEX resource_resource_type_public_id
  ON resource_resource_type (public_id);

CREATE UNIQUE INDEX resource_resource_type_unique
  ON resource_resource_type (resource_id, resource_type_id);

