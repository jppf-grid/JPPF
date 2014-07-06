-- phpMyAdmin SQL Dump
-- version 3.3.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Dec 12, 2010 at 01:48 AM
-- Server version: 5.0.86
-- PHP Version: 5.2.8



/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: 'jppf_samples'
--

-- --------------------------------------------------------

--
-- Table structure for table 'task_result'
--

CREATE TABLE IF NOT EXISTS task_result (
  id int(10) unsigned NOT NULL auto_increment,
  task_id varchar(45) NOT NULL,
  message varchar(255) default NULL,
  PRIMARY KEY  (id)
);

--
-- Dumping data for table 'task_result'
--

