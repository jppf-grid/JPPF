/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestRunListener extends RunListener
{
  /**
   * Holds the results of all tests.
   */
  private final ResultHolder resultHolder;

  /**
   * Initialize this listener witht he specified result holder.
   * @param resultHolder holds the results of all tests.
   */
  public TestRunListener(final ResultHolder resultHolder)
  {
    super();
    if (resultHolder == null) throw new IllegalArgumentException("result holder can't be null");
    this.resultHolder = resultHolder;
  }

  /**
   * Get the object that holds the results of all tests.
   * @return a <code>ResultHolder</code> instance.
   */
  public ResultHolder getResultHolder()
  {
    return resultHolder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testFailure(final Failure failure) throws Exception
  {
    resultHolder.addFailure(failure);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testFinished(final Description description) throws Exception
  {
    if (!resultHolder.hasTest(description)) resultHolder.addSuccess(description);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testIgnored(final Description description) throws Exception
  {
    resultHolder.addIgnored(description);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testRunFinished(final Result result) throws Exception
  {
    resultHolder.setEndTime(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testRunStarted(final Description description) throws Exception
  {
    resultHolder.setStartTime(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void testStarted(final Description description) throws Exception
  {
  }
}
