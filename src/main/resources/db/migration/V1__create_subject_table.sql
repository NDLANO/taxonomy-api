CREATE TABLE subject (
  id        SERIAL PRIMARY KEY,
  public_id VARCHAR(255) NOT NULL,
  name      VARCHAR(255)
);

CREATE UNIQUE INDEX subject_public_id
  ON subject (public_id);

