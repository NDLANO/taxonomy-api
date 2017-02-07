CREATE TABLE subject_translation (
  id            SERIAL PRIMARY KEY,
  subject_id    VARCHAR(255) NOT NULL REFERENCES subject (id),
  language_code VARCHAR(3)   NOT NULL,
  name          VARCHAR(255)
);

CREATE UNIQUE INDEX subject_language_unique
  ON subject_translation (subject_id, language_code);

CREATE TABLE topic_translation (
  id            SERIAL PRIMARY KEY,
  topic_id      VARCHAR(255) NOT NULL REFERENCES topic (id),
  language_code VARCHAR(3)   NOT NULL,
  name          VARCHAR(255)
);

CREATE UNIQUE INDEX topic_language_unique
  ON topic_translation (topic_id, language_code);

CREATE TABLE resource_translation (
  id            SERIAL PRIMARY KEY,
  resource_id   VARCHAR(255) NOT NULL REFERENCES resource (id),
  language_code VARCHAR(3)   NOT NULL,
  name          VARCHAR(255)
);

CREATE UNIQUE INDEX resource_language_unique
  ON resource_translation (resource_id, language_code);

CREATE TABLE resource_type_translation (
  id               SERIAL PRIMARY KEY,
  resource_type_id VARCHAR(255) NOT NULL REFERENCES resource_type (id),
  language_code    VARCHAR(3)   NOT NULL,
  name             VARCHAR(255)
);

CREATE UNIQUE INDEX resource_type_language_unique
  ON resource_type_translation (resource_type_id, language_code);

