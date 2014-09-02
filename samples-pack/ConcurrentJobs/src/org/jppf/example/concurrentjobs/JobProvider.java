/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.concurrentjobs;

import java.io.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.utils.AbstractJPPFJobStream;

/**
 * Instances of this class provide a stream of JPPF jobs, based on the data contained in a text file.
 */
public class JobProvider extends AbstractJPPFJobStream {
  /**
   * The maximum number of tasks in each job.
   */
  private final int tasksPerJob = 10;
  /**
   * Used to read the text file line by line.
   */
  private BufferedReader reader;
  /**
   * Indicates whether there is no more line to reead from the file.
   */
  private boolean eof = false;

  /**
   * Initialize this job provider.
   * @param concurrencyLimit the maximum number of jobs submitted concurrently.
   * @throws IOException if any error occcurs while open the input text file.
   */
  public JobProvider(final int concurrencyLimit) throws IOException {
    super(concurrencyLimit);
    reader = new BufferedReader(new FileReader("input.txt"));
  }

  /**
   * Determine whether there are more jobs to get from the stream.
   * @return {@code true} if there is at least one more job in the stream, {@code false} otherwise.
   */
  @Override
  public boolean hasNext() {
    // return true as long as there is a line to read in the file
    return !eof;
  }

  /**
   * Create the next job in the stream.
   * @return a new {@link JPPFJob} instance whose tasks are created from lines in the input file.
   */
  @Override
  protected JPPFJob createNextJob() {
    JPPFJob job = null;
    try {
      int count = 0;
      // read lines from the file and create a task for each line
      for (int i=0; i<tasksPerJob; i++) {
        String message = reader.readLine();
        if (message == null) {
          // this is the end of stream condition
          eof = true;
          break;
        }
        if (job == null) (job = new JPPFJob()).setName("streaming job " + (getJobCount() + 1));
        // add the task to the job
        job.add(new MyTask(message, 200L)).setId(String.format("%s - task %d", job.getName(), ++count));
      }
    } catch(Exception e) {
      eof = true;
      e.printStackTrace();
    }
    // job == null happens for the last job when the number of lines in the file is a multiple of tasksPerJob
    if ((job == null) || (job.getJobTasks().isEmpty())) {
      eof = true;
      return null;
    }
    return job;
  }

  /**
   * Close the file reader.
   * @throws Exception if any error occurs.
   */
  @Override
  public void close() throws Exception {
    if (reader != null) reader.close();
  }

  /**
   * Process the results of a job that has completed.
   * @param job the job to process.
   */
  @Override
  protected void processResults(final JPPFJob job) {
    ConcurrentJobs.processResults(job);
  }
}
