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

package test.org.jppf.test.runner;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.collections.CollectionUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

/**
 * Holds ther results of a JUnit runner as well as exceptions that occured
 * outside of the runner scope.
 * @author Laurent Cohen
 */
public class ResultHolder implements Serializable
{
  /**
   * Holds exceptions that occurred outside of the JUnit runner.
   */
  private final List<ExceptionHolder> exceptions = new ArrayList<>();
  /**
   * Holds all failures.
   */
  private final Map<String, List<Failure>> failureMap = new TreeMap<>();
  /**
   * Holds all ingored tests.
   */
  private final Map<String, List<Description>> ignoredMap = new TreeMap<>();
  /**
   * Holds all failures.
   */
  private final Map<String, List<Description>> successMap = new TreeMap<>();
  /**
   * A sorted set of classes that were run.
   */
  private final Set<String> classes = new TreeSet<>();
  /**
   * 
   */
  private transient Set<String> tests = new HashSet<>();
  /**
   * The test run start time.
   */
  private long startTime = 0L;
  /**
   * The test run start time.
   */
  private long endTime = 0L;
  /**
   * The count of failed tests.
   */
  private int failureCount = 0;
  /**
   * The count of ignored tests.
   */
  private int ignoredCount = 0;
  /**
   * The count of successful tests.
   */
  private int successCount = 0;

  /**
   * Initialize this result holder.
   */
  public ResultHolder()
  {
  }

  /**
   * Add a new exception.
   * @param holder holds the exception to add.
   */
  public void addException(final ExceptionHolder holder)
  {
    exceptions.add(holder);
  }

  /**
   * Get the exceptions that occurred outside of the JUnit runner.
   * @return a list of <code>ExceptionHolder</code> instances.
   */
  public List<ExceptionHolder> getExceptions()
  {
    return exceptions;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("ResultHolder[");
    sb.append("testsCount=").append(getTestsCount()).append(", failureCount=").append(failureCount).append(", successCount=").append(successCount);
    sb.append(", failures=").append(failureMap).append(", successes=").append(successMap);
    sb.append(", exceptions=").append(exceptions);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Add a test failure.
   * @param failure the failure to add.
   */
  public void addFailure(final Failure failure)
  {
    Description d = failure.getDescription();
    CollectionUtils.putInListMap(d.getClassName(), failure, failureMap);
    processDescription(d);
    failureCount++;
  }

  /**
   * Get the failures.
   * @return a map of failed tests grouped by class name.
   */
  public Map<String, List<Failure>> getFailures()
  {
    return failureMap;
  }

  /**
   * Add an ignored test.
   * @param desc the description of the ignored test.
   */
  public void addIgnored(final Description desc)
  {
    CollectionUtils.putInListMap(desc.getClassName(), desc, ignoredMap);
    processDescription(desc);
    ignoredCount++;
  }

  /**
   * Get the ignored tests.
   * @return a map of ignored tests grouped by class name.
   */
  public Map<String, List<Description>> getIngored()
  {
    return ignoredMap;
  }

  /**
   * Add a successful test.
   * @param desc the description of the successful test.
   */
  public void addSuccess(final Description desc)
  {
    CollectionUtils.putInListMap(desc.getClassName(), desc, successMap);
    processDescription(desc);
    successCount++;
  }

  /**
   * Process the specified test description.
   * @param desc the test description.
   */
  private void processDescription(final Description desc)
  {
    String name = desc.getClassName();
    String testName = name + "." + desc.getMethodName();
    if (!classes.contains(name)) classes.add(name);
    if (!tests.contains(testName)) tests.add(testName);
  }

  /**
   * Get the successful tests.
   * @return a map of successful tests grouped by class name.
   */
  public Map<String, List<Description>> getSuccesses()
  {
    return successMap;
  }

  /**
   * Get the test run start time.
   * @return the start time as a long.
   */
  public long getStartTime()
  {
    return startTime;
  }

  /**
   * Set the test run start time.
   * @param time the start time as a long.
   */
  public void setStartTime(final long time)
  {
    this.startTime = time;
  }

  /**
   * Get the test run end time.
   * @return the end time as a long.
   */
  public long getEndTime()
  {
    return endTime;
  }

  /**
   * Set the test run end time.
   * @param time the end time as a long.
   */
  public void setEndTime(final long time)
  {
    this.endTime = time;
  }

  /**
   * Get the total number of tests.
   * @return the number of tests as an int.
   */
  public int getTestsCount()
  {
    return failureCount + ignoredCount + successCount;
  }

  /**
   * Get the count of failed tests.
   * @return the count as an int.
   */
  public int getFailureCount()
  {
    return failureCount;
  }

  /**
   * Get the count of ignored tests.
   * @return the count as an int.
   */
  public int getIgnoredCount()
  {
    return ignoredCount;
  }

  /**
   * Get the count of successful tests.
   * @return the count as an int.
   */
  public int getSuccessCount()
  {
    return successCount;
  }

  /**
   * Determine whether this result holder already has the specified test.
   * @param desc the description of th etest to look for.
   * @return <code>true</code> if the test already exists, <code>false</code> otherwise.
   */
  public boolean hasTest(final Description desc)
  {
    String name = desc.getClassName() + "." + desc.getMethodName();
    return tests.contains(name);
  }

  /**
   * Get the sorted set of classes that were run.
   * @return a set of string class names.
   */
  public Set<String> getClasses()
  {
    return classes;
  }
}
