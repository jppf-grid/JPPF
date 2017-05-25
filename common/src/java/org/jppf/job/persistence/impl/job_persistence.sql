-- ---------------------------------------
-- Table structure for table `${table}' --
-- ---------------------------------------

CREATE TABLE ${table} (
  UUID varchar(250) NOT NULL,
  TYPE varchar(20) NOT NULL,
  POSITION int NOT NULL,
  CONTENT blob NOT NULL,
  PRIMARY KEY (UUID, TYPE, POSITION)
);
