CREATE TABLE resource (
  id        SERIAL PRIMARY KEY,
  public_id VARCHAR(255) NOT NULL,
  name      VARCHAR(255)
);

CREATE UNIQUE INDEX resource_public_id
  ON resource (public_id);

