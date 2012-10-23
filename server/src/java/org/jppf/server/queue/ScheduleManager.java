/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.queue;

import java.text.ParseException;

import org.jppf.scheduling.*;
import org.jppf.server.protocol.ServerJob;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @author Martin JANDA
 */
class ScheduleManager
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ScheduleManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Handles the schedule of each job that has one.
   */
  private final JPPFScheduleHandler jobScheduleHandler = new JPPFScheduleHandler("Job Schedule Handler");
  /**
   * Handles the expiration schedule of each job that has one.
   */
  private final JPPFScheduleHandler jobExpirationHandler = new JPPFScheduleHandler("Job Expiration Handler");

  /**
   * Process the start schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  void handleStartJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobSchedule();
    if (schedule != null)
    {
      bundleWrapper.setPending(true);
      String jobId = bundleWrapper.getName();
      final String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
      try
      {
        long dt = bundleWrapper.getJobReceivedTime();
        jobScheduleHandler.scheduleAction(uuid, schedule, new JobScheduleAction(bundleWrapper), dt);
      }
      catch(ParseException e)
      {
        bundleWrapper.setPending(false);
        log.error("Unparseable start date for job id " + jobId + " : date = " + schedule.getDate() + ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    }
    else
    {
      bundleWrapper.setPending(false);
    }
  }

  /**
   * Process the expiration schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  void handleExpirationJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobExpirationSchedule();
    if (schedule != null)
    {
      String jobId = bundleWrapper.getName();
      final String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
      long dt = bundleWrapper.getJobReceivedTime();
      try
      {
        jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(bundleWrapper), dt);
      }
      catch(ParseException e)
      {
        log.error("Unparsable expiration date for job id " + jobId + " : date = " + schedule.getDate() +
                ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    }
  }

  /**
   * Clear all the scheduled actions associated with a job.
   * This method should normally only be called when a job has completed.
   * @param jobUuid the job uuid.
   */
  void clearSchedules(final String jobUuid)
  {
    jobScheduleHandler.cancelAction(jobUuid);
    jobExpirationHandler.cancelAction(jobUuid);
  }

  /**
   * Close this this schedule manager and all resources it uses.
   */
  void close()
  {
    jobScheduleHandler.clear(true);
    jobExpirationHandler.clear(true);
  }
}
