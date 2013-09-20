/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package test.multical;

import java.io.Serializable;
import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 */
public class Multiplecal implements Serializable {
  /**
   */
  private static JPPFClient client = null;

  /**
   * @param args .
   */
  public static void main(final String[] args) {
    try {
      System.out.println("value of 220 = " + 2.2d * 100);
      client = new JPPFClient();
      System.out.println("Submitting job 1");
      List<JPPFTask> results = submit_1();
      printResults(results);
      System.out.println("Submitting job 2");
      results = submit_2();
      printResults(results);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * @return .
   * @throws Exception .
   */
  private static List<JPPFTask> submit_1() throws Exception {
    JPPFJob job = new JPPFJob();
    job.addTask(new AddTwo(5));
    job.addTask(new AddThree(6));
    job.addTask(new AddFour(7));
    return client.submit(job);
  }


  /**
   * @return .
   * @throws Exception .
   */
  private static List<JPPFTask> submit_2() throws Exception {
    JPPFJob job = new JPPFJob();
    Multiplecal calobj = new Multiplecal();
    job.addTask("addtwo", calobj, 5);
    job.addTask("addthree", calobj, 6);
    job.addTask("addfour", calobj, 7);
    return client.submit(job);
  }

  /**
   * @param results .
   */
  private static void printResults(final List<JPPFTask> results) {
    int count = 0;
    for (JPPFTask task: results) {
      count++;
      if (task.getException() != null) task.getException().printStackTrace();
      else System.out.println("task #" + count + " result = " + task.getResult());
    }
  }
  /**
   * @param n .
   * @return .
   */
  public int addtwo(final int n) {
    return (n + (n - 1));
  }

  /**
   * @param n .
   * @return .
   */
  public int addthree(final int n) {
    return (n + (n - 1) + (n - 2));
  }

  /**
   * @param n .
   * @return .
   */
  public int addfour(final int n) {
    return (n + (n - 1) + (n - 2) + (n - 3));
  }

  /**
   */
  public static class AddTwo extends JPPFTask {
    /**
     */
    private int n;

    /**
     * @param n .
     */
    public AddTwo(final int n) {
      this.n = n;
    }

    @Override
    public void run() {
      setResult(n + (n - 1));
    }
  }

  /**
   */
  public static class AddThree extends JPPFTask {
    /**
     */
    private int n;

    /**
     * @param n .
     */
    public AddThree(final int n) {
      this.n = n;
    }

    @Override
    public void run() {
      setResult(n + (n - 1) + (n - 2));
    }
  }

  /**
   */
  public static class AddFour extends JPPFTask {
    /**
     */
    private int n;

    /**
     * @param n .
     */
    public AddFour(final int n) {
      this.n = n;
    }

    @Override
    public void run() {
      setResult(n + (n - 1) + (n - 2) + (n - 3));
    }
  }
}