/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package sample.dist.tasklength;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class LongTaskRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LongTaskRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      jppfClient = new JPPFClient();
      TypedProperties props = JPPFConfiguration.getProperties();
      int length = props.getInt("longtask.length");
      int nbTask = props.getInt("longtask.number");
      int iterations = props.getInt("longtask.iterations");
      print("Running Long Task demo with "+nbTask+" tasks of length = "+length+" ms for "+iterations+" iterations");
      perform(nbTask, length, iterations);
      //performAsync(nbTask, length, iterations);
      //perform3(nbTask, length, iterations);
      //perform4();
      //perform5();
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int nbTasks, final int length, final int iterations) throws Exception {
    // perform "iteration" times
    long totalTime = 0L;
    for (int iter=1; iter<=iterations; iter++) {
      long start = System.nanoTime();
      JPPFJob job = new JPPFJob();
      job.setName("Long task iteration " + iter);
      for (int i=0; i<nbTasks; i++) job.add(new LongTask(length)).setId("" + iter + ':' + (i+1));
      // submit the tasks for execution
      List<Task<?>> results = jppfClient.submitJob(job);
      for (Task<?> task: results) {
        Throwable e = task.getThrowable();
        if (e != null) {
          if (e instanceof Exception) throw (Exception) e;
          else throw new JPPFException(e);
        }
      }
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      print("Iteration #" + iter + " performed in " + StringUtils.toStringDuration(elapsed));
      totalTime += elapsed;
    }
    print("Average iteration time: " + StringUtils.toStringDuration(totalTime/iterations));
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * A <code>Callable</code> wrapper around a <code>JPPFTask</code>.
   */
  public static class JPPFTaskCallable extends AbstractTask<Object> implements JPPFCallable<Object> {
    /**
     * The task to run.
     */
    private AbstractTask<?> task = null;

    /**
     * Initialize this callable with the specified jppf task.
     * @param task a <code>JPPFTask</code> instance.
     */
    public JPPFTaskCallable(final AbstractTask<?> task) {
      this.task = task;
    }

    @Override
    public void run() {
      task.run();
      setResult(task.getResult());
      setThrowable(task.getThrowable());
    }

    @Override
    public Object call() throws Exception {
      run();
      return getResult();
    }
  }
}
