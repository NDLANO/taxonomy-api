CREATE TABLE cached_url (
  id          SERIAl PRIMARY KEY,
  public_id   VARCHAR (255),
  path        VARCHAR (255) NOT NULL,
  is_primary  BOOLEAN
);