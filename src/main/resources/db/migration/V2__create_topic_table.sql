CREATE TABLE topic (
  id        SERIAL PRIMARY KEY,
  public_id VARCHAR(255) NOT NULL,
  name      VARCHAR(255)
);

CREATE UNIQUE INDEX topic_public_id
  ON topic (public_id);

