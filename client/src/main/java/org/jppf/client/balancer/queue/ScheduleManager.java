/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.client.balancer.queue;

import java.text.ParseException;

import org.jppf.client.balancer.ClientJob;
import org.jppf.scheduling.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Manages the start and expiration schedules for jobs added to the queue.
 * @author Laurent Cohen
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
   * @param clientJob the job to process.
   */
  void handleStartJobSchedule(final ClientJob clientJob) {
    final String uuid = clientJob.getUuid();
    if (jobStartHandler.hasAction(uuid)) return;
    final JPPFSchedule schedule = clientJob.getClientSLA().getJobSchedule();
    if (schedule != null) {
      clientJob.setPending(true);
      final String name = clientJob.getName();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + name);
      try {
        final long dt = clientJob.getJobReceivedTime();
        jobStartHandler.scheduleAction(uuid, schedule, new JobScheduleAction(clientJob), dt);
        clientJob.addOnDone(() -> jobStartHandler.cancelAction(uuid));
      } catch (final ParseException e) {
        clientJob.setPending(false);
        log.error("Unparseable start date for job '{}' : date = {}, date format = {}\n{}", name , schedule.getDate(), schedule.getFormat(), ExceptionUtils.getStackTrace(e));
      }
    } else {
      clientJob.setPending(false);
    }
  }

  /**
   * Process the expiration schedule specified in the job SLA.
   * @param clientJob the job to process.
   */
  void handleExpirationJobSchedule(final ClientJob clientJob) {
    final String uuid = clientJob.getUuid();
    if (jobExpirationHandler.hasAction(uuid)) return;
    final JPPFSchedule schedule = clientJob.getClientSLA().getJobExpirationSchedule();
    if (schedule != null) {
      final String name = clientJob.getName();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + name);
      final long dt = clientJob.getJobReceivedTime();
      try {
        jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(clientJob), dt);
        clientJob.addOnDone(() -> jobExpirationHandler.cancelAction(uuid));
      } catch (final ParseException e) {
        log.error("Unparseable expiration date for job '{}' : date = {}, date format = {}\n{}", name , schedule.getDate(), schedule.getFormat(), ExceptionUtils.getStackTrace(e));
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
