JPPF makes it easy to parallelize computationally intensive tasks and execute them on a Grid

What's new in JPPF 3.3_beta

Bug fixes	

JPPF-107 Missing exception handling upon invocation of NodeLifeCycleListener methods
JPPF-110 Proportional algorithm results in uneven load with small number of long-lived tasks
JPPF-113 NullPointerException in the console upon connection to first driver
JPPF-114 Missing unit tests for node management and monitoring APIs
JPPF-116 NPE in AbstractJPPFClassLoader.findResources()
JPPF-117 Task timeout is not working as expected
JPPF-120 [Regression] Impossible to make an SSL connection to the server
JPPF-122 JMX connection threads leak in the driver
JPPF-123 NPE at driver startup when jppf.ssl.server.port = -1
JPPF-125 Selection in job data panel does not update the buttons state
JPPF-126 Job cancelled from the admin console may get stuck in the server queue

Enhancements 

JPPF-68 Node: separation of class loader instances and driver connection
JPPF-69 Minimize usage of static fields/instances in JPPF
JPPF-104 Inconsistent naming of the load-balancing properties
JPPF-118 Improvements in NodeExecutionManagerImpl
JPPF-124 Refactor the \"Extended Class Loading\" sample to use class loader reset ability

Feature requests 

JPPF-24 Ability to deactivate a node
JPPF-26 Enable node management via the driver connection
JPPF-38 Use SplashScreen support from Java
JPPF-109 Pluggable error handler for NodeLifeCycleListener
JPPF-112 Refactor the admin console for management and monitoring via the driver

To browse an issue, use the URL http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-<issue_number>

JPPF Links:

Web Site: http://www.jppf.org
Downloads: http://www.jppf.org/downloads.php
Documentation: http://www.jppf.org/wiki
User forums: http://www.jppf.org/forums
Issue tracker: http://www.jppf.org/tracker/tbg/jppf
SF.net project page: http://sourceforge.net/projects/jppf-project/
