/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
class ScheduleManager {
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
  private final JPPFScheduleHandler jobStartHandler = new JPPFScheduleHandler("Job Schedule Handler");
  /**
   * Handles the expiration schedule of each job that has one.
   */
  private final JPPFScheduleHandler jobExpirationHandler = new JPPFScheduleHandler("Job Expiration Handler");

  /**
   * Process the start schedule specified in the job SLA.
   * @param serverJob the job to process.
   */
  void handleStartJobSchedule(final ServerJob serverJob) {
    final String uuid = serverJob.getUuid();
    if (jobStartHandler.hasAction(uuid)) return;
    JPPFSchedule schedule = serverJob.getSLA().getJobSchedule();
    if (schedule != null) {
      serverJob.setPending(true);
      String jobId = serverJob.getName();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
      try {
        long dt = serverJob.getJobReceivedTime();
        jobStartHandler.scheduleAction(uuid, schedule, new JobScheduleAction(serverJob), dt);
      } catch (ParseException e) {
        serverJob.setPending(false);
        log.error("Unparseable start date for job id " + jobId + " : date = " + schedule.getDate() + ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    } else {
      serverJob.setPending(false);
    }
  }

  /**
   * Process the expiration schedule specified in the job SLA.
   * @param serverJob the job to process.
   */
  void handleExpirationJobSchedule(final ServerJob serverJob) {
    final String uuid = serverJob.getUuid();
    if (jobExpirationHandler.hasAction(uuid)) return;
    JPPFSchedule schedule = serverJob.getSLA().getJobExpirationSchedule();
    if (schedule != null) {
      String jobId = serverJob.getName();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
      long dt = serverJob.getJobReceivedTime();
      try {
        jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(serverJob), dt);
      } catch (ParseException e) {
        log.error("Unparsable expiration date for job id " + jobId + " : date = " + schedule.getDate() + ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    }
  }

  /**
   * Clear all the scheduled actions associated with a job.
   * This method should normally only be called when a job has completed.
   * @param jobUuid the job uuid.
   */
  void clearSchedules(final String jobUuid) {
    jobStartHandler.cancelAction(jobUuid);
    jobExpirationHandler.cancelAction(jobUuid);
  }

  /**
   * Close this this schedule manager and all resources it uses.
   */
  void close() {
    jobStartHandler.clear(true);
    jobExpirationHandler.clear(true);
  }
}
