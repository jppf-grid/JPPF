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

package test.org.jppf.test.runner;

import java.io.PrintStream;

import org.jppf.utils.streams.StreamUtils;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestRunListener extends RunListener {
  /**
   * Holds the results of all tests.
   */
  private final ResultHolder resultHolder;
  /**
   * Used to log during the test runs.
   */
  private PrintStream out = null;
  /**
   * The default System.out.
   */
  private PrintStream defaultSysout = null;
  /**
   * The default System.err.
   */
  private PrintStream defaultSyserr = null;
  /**
   * 
   */
  private boolean isLogging = false;
  /**
   * The test class being executed.
   */
  private String currentClass = "";
  /**
   * Enum of possible action upon test failure.
   */
  private enum FailureAction {
    /**
     * No action: just continue running the tests.
     */
    NONE,
    /**
     * Exit with a System.exit(1), keeps the logs in the state of the last (and first) failure.
     */
    EXIT,
    /**
     * Wait until user presses [Enter].
     */
    WAIT
  }
  /**
   * What to do upon test failure.
   * EXIT, WAIT, or NONE
   */
  private FailureAction actionOnError = FailureAction.NONE;

  /**
   * Initialize this listener with the specified result holder.
   * @param resultHolder holds the results of all tests.
   */
  public TestRunListener(final ResultHolder resultHolder) {
    this(resultHolder, null);
  }

  /**
   * Initialize this listener with the specified result holder.
   * @param resultHolder holds the results of all tests.
   * @param out used to log the results during execution of the tests.
   */
  public TestRunListener(final ResultHolder resultHolder, final PrintStream out) {
    if (resultHolder == null) throw new IllegalArgumentException("result holder can't be null");
    this.resultHolder = resultHolder;
    this.out = out;
    isLogging = (out != null);
    String s = System.getProperty("failure.action", "NONE");
    for (FailureAction action: FailureAction.values()) {
      if (action.name().equalsIgnoreCase(s)) {
        actionOnError = action;
        break;
      }
    }
  }

  /**
   * Get the object that holds the results of all tests.
   * @return a <code>ResultHolder</code> instance.
   */
  public ResultHolder getResultHolder() {
    return resultHolder;
  }

  @Override
  public void testRunStarted(final Description description) throws Exception {
    resultHolder.setStartTime(System.currentTimeMillis());
    if (isLogging) {
      defaultSysout = System.out;
      defaultSyserr = System.err;
      System.setOut(out);
      System.setErr(out);
    }
  }

  @Override
  public void testRunFinished(final Result result) throws Exception {
    resultHolder.setEndTime(System.currentTimeMillis());
    if (isLogging) {
      System.setOut(defaultSysout);
      System.setErr(defaultSyserr);
    }
  }

  @Override
  public void testStarted(final Description description) throws Exception {
    if (isLogging) {
      if (!description.getClassName().equals(currentClass)) {
        currentClass = description.getClassName();
        out.println("\n---------- end of output for class " + currentClass + " ----------\n");
        defaultSysout.println("class " + currentClass);
      }
      //out.println("----- " + description.getMethodName() + " -----");
      defaultSysout.println("  * " + description.getMethodName());
    }
  }

  @Override
  public void testFailure(final Failure failure) throws Exception {
    resultHolder.addFailure(failure);
    if (isLogging) defaultSysout.println("  - " + failure.getDescription().getMethodName() + " : Failure '" + failure.getMessage() + "'");
    if (actionOnError == FailureAction.EXIT) System.exit(1);
    if (actionOnError == FailureAction.WAIT) StreamUtils.waitKeyPressed("Press [Enter] to continue ...");
  }

  @Override
  public void testFinished(final Description description) throws Exception {
    if (!resultHolder.hasTest(description)) {
      resultHolder.addSuccess(description);
      if (isLogging) defaultSysout.println("  + " + description.getMethodName() + " : OK");
    }
  }

  @Override
  public void testIgnored(final Description description) throws Exception {
    resultHolder.addIgnored(description);
  }
}
