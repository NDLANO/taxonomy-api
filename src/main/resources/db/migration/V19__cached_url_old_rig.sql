CREATE TABLE cached_url_old_rig (
  id        SERIAL PRIMARY KEY,
  old_url   VARCHAR(255) NOT NULL,
  new_path  VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX cached_url_old_rig_id
  ON cached_url_old_rig (old_url);

 