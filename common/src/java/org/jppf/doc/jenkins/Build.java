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

package org.jppf.doc.jenkins;

import org.jppf.utils.StringUtils;

/**
 *
 * @author Laurent Cohen
 */
public class Build {
  /**
   * The build number.
   */
  private int number;
  /**
   * The build start time.
   */
  private long startTime;
  /**
   * The build duration in millis.
   */
  private long duration;
  /**
   * The build result: "SUCCESS" or "FAILURE".
   */
  private String result;
  /**
   * The tests results.
   */
  private TestResults testResults;

  /**
   * Get the build number.
   * @return the build number as an int.
   */
  public int getNumber() {
    return number;
  }

  /**
   * Set the build number.
   * @param number the build number as an int.
   */
  public void setNumber(final int number) {
    this.number = number;
  }

  /**
   * Get the build start time.
   * @return the build start time as a long.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Set the build start time.
   * @param startTime the build start time as a long.
   */
  public void setStartTime(final long startTime) {
    this.startTime = startTime;
  }

  /**
   * Get the build duration in milliseconds.
   * @return the build duration as a long.
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Set the build duration in milliseconds.
   * @param duration the build duration as a long.
   */
  public void setDuration(final long duration) {
    this.duration = duration;
  }

  /**
   * Get the build result.
   * @return either "SUCCESS" or "FAILURE".
   */
  public String getResult() {
    return result;
  }

  /**
   * Set the build result.
   * @param result either "SUCCESS" or "FAILURE".
   */
  public void setResult(final String result) {
    this.result = result;
  }

  /**
   * Get the tests results.
   * @return a TestResults instance.
   */
  public TestResults getTestResults() {
    return testResults;
  }

  /**
   * Set the tests results.
   * @param testResults a TestResults instance.
   */
  public void setTestResults(final TestResults testResults) {
    this.testResults = testResults;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("number=").append(number)
      .append(", result=").append(result)
      .append(", startTime=").append(startTime)
      .append(", duration=").append(StringUtils.toStringDuration(duration))
      .append(", testResults=").append(testResults)
      .append(']')
      .toString();
  }
}
