package org.jppf.job;

/**
 * Created with IntelliJ IDEA.
 * User: jandam
 * Date: 9/4/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JobNotificationEmitter {

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  public void fireJobEvent(final JobNotification event);
}
