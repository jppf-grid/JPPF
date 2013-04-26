-- phpMyAdmin SQL Dump
-- version 2.11.0
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Mar 24, 2010 at 08:34 AM
-- Server version: 5.0.86
-- PHP Version: 5.2.8

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `pervasiv_jppfweb`
--

-- --------------------------------------------------------

--
-- Table structure for table `links`
--

CREATE TABLE IF NOT EXISTS `links` (
  `group_id` int(9) NOT NULL default '0',
  `link_id` int(9) NOT NULL default '0',
  `title` varchar(255) default NULL,
  `desc` mediumtext,
  `url` varchar(255) default NULL,
  PRIMARY KEY  (`group_id`,`link_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `links`
--

INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(1, 1, 'Cluster Monkey', 'Cluster Monkey provides a lot of exciting content on clustering: articles, news, conferences reports, links, etc...', 'http://www.clustermonkey.net/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(1, 2, 'Beowulf.org', 'Beowulf.org is a collection of resources for the expanding universe of users and designers of Beowulf class cluster computers. These enterprise systems are built on commodity hardware deploying Linux OS and open source software.', 'http://www.beowulf.org/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(1, 3, 'Terracotta', 'Terracotta is Network Attached Memory (NAM). NAM is best suited for storing what we like to call scratch data. Scratch data is defined as object oriented data that is critical to the execution of a series of Java operations inside the JVM, but may not be critical once a business transaction is complete. Some examples of scratch data include business workflow state (which step of the flow is the system currently working on), or HTML form data that is being validated by a web application before a database record is created or updated.', 'http://www.terracotta.org');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 1, 'TeraGrid', 'TeraGrid is an open scientific discovery infrastructure combining leadership class resources at eight partner sites to create an integrated, persistent computational resource. ', 'http://teragrid.org/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 2, 'The Globus Alliance', 'The Globus Alliance is a community of organizations and individuals developing fundamental technologies behind the "Grid," which lets people share computing power, databases, instruments, and other on-line tools securely across corporate, institutional, and geographic boundaries without sacrificing local autonomy.', 'http://www.globus.org');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 3, 'World Community Grid', ' World Community Grid''s mission is to create the largest public computing grid benefiting humanity. Our work is built on the belief that technological innovation combined with visionary scientific research and large-scale volunteerism can change our world for the better. Our success depends on individuals - like you - collectively contributing their unused computer time to this not-for-profit endeavor.', 'http://www.worldcommunitygrid.org');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 4, 'Grid.org', 'Grid.org is a single destination site for large-scale research projects powered by the United Devices grid computing solution, Grid MP Global. From the Cancer Research Project sponsored by Intel and the University of Oxford to the Anthrax Research Project sponsored by Intel and Microsoft, the Grid MP Global @ grid.org has been put to use for research and analysis projects of groundbreaking scope. With the participation of over 2 million devices worldwide, grid.org projects driven by the Grid MP Global have achieved record levels of speed and success in processing data.', 'http://www.grid.org');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 6, 'Israeli Association Of Grid Technologies', 'The IGT is a non-profit organization of leading vendors, ISVs, customers and academia, focused on knowledge sharing and networking for developing Enterprise Grid, Virtualization, SOA and SOI solutions. It is open, independent and vendor-neutral.', 'http://www.grid.org.il');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 7, 'Open Grid Forum', 'The Open Grid Forum (OGF) is a community of users, developers, and vendors leading the global standardization effort for grid computing. The OGF community consists of thousands of individuals in industry and research, representing over 400 organizations in more than 50 countries. Together we work to accelerate adoption of grid computing worldwide because we believe grids will lead to new discoveries, new opportunities, and better business practices. ', 'http://www.ogf.org');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(2, 9, 'GIGASPACES', 'GigaSpaces provides infrastructure software solutions that deliver unparalleled dynamic scalability for high-volume transactional applications, without the overhead and complexity inherent in traditional multi-tier development & deployment environments.<br>Its award-winning solutions are being adopted across vertical industries such as financial services, telecommunications and law-enforcement for mission-critical applications, where the need for extreme performance, reliability and scalability necessitates an alternative to traditional tier-based architectures. ', 'http://www.gigaspaces.com');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(3, 1, 'IEEE Distributed Systems Online', 'IEEE Distributed Systems Online is a springboard for building a stronger distributed systems community and a forum for sharing ideas and discussing projects.', 'http://dsonline.computer.org/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(3, 2, 'JCyclone', 'JCyclone is a Staged Event-Driven Architecture (SEDA) based implementation made in Java.', 'http://jcyclone.sourceforge.net/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(3, 3, 'Condor', 'The goal of the Condor Project is to develop, implement, deploy, and evaluate mechanisms and policies that support High Throughput Computing (HTC) on large collections of distributively owned computing resources. Guided by both the technological and sociological challenges of such a computing environment, the Condor Team has been building software tools that enable scientists and engineers to increase their computing throughput. ', 'http://www.cs.wisc.edu/condor/');
INSERT INTO `links` (`group_id`, `link_id`, `title`, `desc`, `url`) VALUES(4, 1, 'LinuxHPC.org', 'LinuxHPC.org is a website for System Administrators, developers, and enterprise managers, offering recent industry news, events, mailing lists and links, etc. related to high performance technical computing and clustering with Linux. ', 'http://www.linuxhpc.org/');
