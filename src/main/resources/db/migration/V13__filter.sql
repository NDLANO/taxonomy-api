CREATE TABLE filter (
  id          SERIAl PRIMARY KEY,
  public_id   VARCHAR (255) NOT NULL,
  name        VARCHAR (255)
);

CREATE UNIQUE INDEX filter_public_id
  ON filter (public_id);

CREATE TABLE filter_translation (
  id            SERIAL PRIMARY KEY,
  filter_id    INT        NOT NULL REFERENCES filter (id),
  language_code VARCHAR(3) NOT NULL,
  name          VARCHAR(255)
);