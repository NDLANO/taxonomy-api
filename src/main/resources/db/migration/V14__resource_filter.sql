CREATE TABLE relevance (
  id        SERIAL PRIMARY KEY,
  public_id VARCHAR(255) NOT NULL,
  name      VARCHAR(255)
);

CREATE UNIQUE INDEX relevance_public_id
  ON relevance (public_id);

CREATE TABLE relevance_translation (
  id            SERIAL PRIMARY KEY,
  relevance_id  INT        NOT NULL,
  language_code VARCHAR(3) NOT NULL,
  name          VARCHAR(255)
);

CREATE UNIQUE INDEX relevance_language_unique
  ON relevance_translation (relevance_id, language_code);

CREATE TABLE resource_filter (
  id           SERIAL PRIMARY KEY,
  public_id    VARCHAR(255) NOT NULL,
  resource_id  INT REFERENCES resource (id),
  filter_id    INT REFERENCES filter (id),
  relevance_id INT REFERENCES relevance (id)
);

CREATE UNIQUE INDEX resource_filter_public_id
  ON resource_filter (public_id);

CREATE UNIQUE INDEX resource_filter_unique
  ON resource_filter (filter_id, resource_id);

