/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.test.scenario.classloading;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.slf4j.*;

import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Testing the resubmission of a job when the driver is disconnected.
 * @author Laurent Cohen
 */
public class Runner extends AbstractScenarioRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(Runner.class);

  @Override
  public void run() {
    try {
      final TypedProperties config = getConfiguration().getProperties();
      final String file = config.getString("class.names.file");
      final List<String> list = FileUtils.getFilePathList(file);
      final long start = System.nanoTime();
      final JPPFJob job = BaseTestHelper.createJob("classloading", true, true, 1, MyTask.class);
      final DataProvider dp = new MemoryMapDataProvider();
      dp.setParameter("list", list);
      job.setDataProvider(dp);
      getSetup().getClient().submitJob(job);
      final long elapsed = System.nanoTime() - start;
      output(job.getName() + " done in " + StringUtils.toStringDuration(elapsed / 1000000L));
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message - the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }

  /**
   * 
   */
  public static class MyTask extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
      int nbSuccess = 0;
      List<String> list = null;
      try {
        list = (List<String>) getDataProvider().getParameter("list");
      } catch (final Exception e) {
        setThrowable(e);
        e.printStackTrace();
        return;
      }
      boolean first = true;
      int nbClasses = 0;
      final ClassLoader cl = getClass().getClassLoader();
      for (String s: list) {
        nbClasses++;
        try {
          Class.forName(s, true, cl);
          nbSuccess++;
        } catch (final Throwable t) {
          if (!first) {
            first = true;
            System.out.println("throwable for '" + s + "' : " + ExceptionUtils.getStackTrace(t));
          }
        }
      }
      System.out.println("total classes=" + nbClasses + ", success=" + nbSuccess);
    }
  }
}
