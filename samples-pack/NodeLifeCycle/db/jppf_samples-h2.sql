-- --------------------------------------------------------
-- Database: 'jppf_samples'

-- --------------------------------------------------------
-- Create user jppf with admin role
-- --------------------------------------------------------

CREATE USER IF NOT EXISTS jppf PASSWORD 'jppf' ADMIN;

-- --------------------------------------------------------
-- Table structure for table 'task_result'
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS task_result (
  id IDENTITY,
  task_id varchar(45) NOT NULL,
  message varchar(255) default NULL,
  PRIMARY KEY  (id)
);
