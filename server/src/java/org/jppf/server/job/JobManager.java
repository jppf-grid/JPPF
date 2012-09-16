package org.jppf.server.job;

import org.jppf.job.JobListener;
import org.jppf.server.protocol.ServerJob;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jandam
 * Date: 9/4/12
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JobManager {
  /**
   * Cancel the job with the specified UUID
   *
   * @param jobId the uuid of the job to cancel.
   * @return whether cancellation was successful.
   */
  boolean cancelJob(final String jobId) throws Exception;

//  /**
//   * Suspend or resume job with the specified UUID
//   *
//   * @param jobId the uuid of the job to suspend/resume.
//   * @param suspend
//   * @return whether action was successful.
//   */
//  public boolean setSuspendJob(final String jobId, final boolean suspend) throws Exception;

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  void addJobListener(final JobListener listener);

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  void removeJobListener(final JobListener listener);

  ServerJob getBundleForJob(final String jobUuid);

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  void updatePriority(final String jobUuid, final int newPriority);

  Set<String> getAllJobIds();
}
