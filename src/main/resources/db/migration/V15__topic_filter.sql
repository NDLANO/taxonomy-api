CREATE TABLE topic_filter (
  id           SERIAL PRIMARY KEY,
  public_id    VARCHAR(255) NOT NULL,
  topic_id  INT REFERENCES topic (id),
  filter_id    INT REFERENCES filter (id),
  relevance_id INT REFERENCES relevance (id)
);

CREATE UNIQUE INDEX topic_filter_public_id
  ON topic_filter (public_id);

CREATE UNIQUE INDEX topic_filter_unique
  ON topic_filter (filter_id, topic_id);

