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

package test.org.jppf.serialization;

import org.junit.Test;

import test.org.jppf.test.setup.AbstractNonStandardSetup;

/**
 * Unit tests for the JPPF serialization scheme.
 * @author Laurent Cohen
 */
public abstract class AbstractTestSerialization extends AbstractNonStandardSetup {
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 15_000L;
  /**
   * Whether the serialization scheme allows non-serializable classes.
   */
  static boolean allowsNonSerializable = true;

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testSimpleJob() throws Exception {
    super.testSimpleJob(null);
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testMultipleJobs() throws Exception {
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelJob() throws Exception {
    super.testCancelJob();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testNotSerializableWorkingInNode() throws Exception {
    if (allowsNonSerializable) super.testNotSerializableWorkingInNode();
  }

  @Override
  @Test(timeout = TEST_TIMEOUT)
  public void testForwardingMBean() throws Exception {
    super.testForwardingMBean();
  }
}
