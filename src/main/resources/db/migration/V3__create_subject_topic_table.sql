CREATE TABLE subject_topic (
  id         SERIAL PRIMARY KEY,
  public_id  VARCHAR(255) NOT NULL,
  topic_id   INT REFERENCES topic (id),
  subject_id INT REFERENCES subject (id),
  is_primary BOOLEAN
);

CREATE UNIQUE INDEX subject_topic_public_id
  ON subject_topic (public_id);

CREATE UNIQUE INDEX subject_topic_unique
  ON subject_topic (topic_id, subject_id);

