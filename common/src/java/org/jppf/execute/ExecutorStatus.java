package org.jppf.execute;

/**
* Created with IntelliJ IDEA.
* User: jandam
* Date: 9/5/12
* Time: 12:31 PM
* To change this template use File | Settings | File Templates.
*/
public enum ExecutorStatus {
  /**
   * The client is successfully connected to the driver.
   */
  ACTIVE,
  /**
   * The connection is currently executing a job.
   */
  EXECUTING,
  /**
   * The client failed to connect to the driver and no further attempt will be made.
   */
  FAILED,
  DISABLED
}
