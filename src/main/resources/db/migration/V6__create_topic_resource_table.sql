CREATE TABLE topic_resource (
  id          SERIAL PRIMARY KEY,
  public_id   VARCHAR(255) NOT NULL,
  topic_id    INT REFERENCES topic (id),
  resource_id INT REFERENCES resource (id),
  is_primary  BOOLEAN
);

CREATE UNIQUE INDEX topic_resource_public_id
  ON topic_resource (public_id);

CREATE UNIQUE INDEX topic_resource_unique
  ON topic_resource (topic_id, resource_id);

