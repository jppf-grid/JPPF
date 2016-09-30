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

package org.jppf.doc.jenkins;

/**
 *
 * @author Laurent Cohen
 */
public class TestResults {
  /**
   * The total number of tests, including those skipped.
   */
  private int totalCount;
  /**
   * The number of failed tests.
   */
  private int failures;
  /**
   * The number of skipped tests.
   */
  private int skipped;

  /**
   * Get the total number of tests, including those skipped.
   * @return the number of tests as an int.
   */
  public int getTotalCount() {
    return totalCount;
  }

  /**
   * Set the total number of tests, including those skipped.
   * @param totalCount the number of tests as an int.
   */
  public void setTotalCount(final int totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * Get the number of failed tests.
   * @return the number of failed tests as an int.
   */
  public int getFailures() {
    return failures;
  }

  /**
   * Set the number of failed tests.
   * @param failures the number of failed tests as an int.
   */
  public void setFailures(final int failures) {
    this.failures = failures;
  }

  /**
   * Get the number of skipped tests.
   * @return the number of skipped tests as an int.
   */
  public int getSkipped() {
    return skipped;
  }

  /**
   * Set the number of skipped tests.
   * @param skipped the number of skipped tests as an int.
   */
  public void setSkipped(final int skipped) {
    this.skipped = skipped;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("totalCount=").append(totalCount)
      .append(", failures=").append(failures)
      .append(", skipped=").append(skipped)
      .append(']')
      .toString();
  }
}
