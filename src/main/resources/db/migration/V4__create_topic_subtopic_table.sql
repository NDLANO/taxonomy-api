CREATE TABLE topic_subtopic (
  id          SERIAL PRIMARY KEY,
  public_id   VARCHAR(255) NOT NULL,
  topic_id    INT REFERENCES topic (id),
  subtopic_id INT REFERENCES topic (id),
  is_primary  BOOLEAN
);

CREATE UNIQUE INDEX topic_subtopic_public_id
  ON topic_subtopic (public_id);

CREATE UNIQUE INDEX topic_subtopic_unique
  ON topic_subtopic (topic_id, subtopic_id);

