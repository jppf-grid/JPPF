-- ---------------------------------------
-- Table structure for table `${table}' --
-- ---------------------------------------

CREATE TABLE ${table} (
  NODEID varchar(250) NOT NULL,
  ALGORITHMID varchar(250) NOT NULL,
  STATE blob NOT NULL,
  PRIMARY KEY (NODEID, ALGORITHMID)
);
