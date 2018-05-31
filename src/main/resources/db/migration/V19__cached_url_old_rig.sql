CREATE TABLE cached_url_old_rig (
  url       VARCHAR(255) PRIMARY KEY,
  public_id VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX cached_url_old_rig_id
  ON cached_url_old_rig (url);

 