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

package test.org.jppf.utils;

import static org.junit.Assert.*;

import org.jppf.JPPFTimeoutException;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit test for the <code>TypedProperties</code> class.
 * @author Laurent Cohen
 */
public class TestConcurrentUtils extends BaseTest {
  /** */
  Condition1 cond1;

  /**
   * Test a call to {@link ConcurrentUtils#awaitInterruptibleCondition(ConcurrentUtils.Condition, long, boolean) awaitInterruptibleCondition()} which results in a {@link JPPFTimeoutException}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L, expected=JPPFTimeoutException.class)
  public void testAwaitInteruptibleTimeout() throws Exception {
    assertFalse(ConcurrentUtils.awaitInterruptibleCondition(cond1 = new Condition1(false), 500L, true));
    assertTrue(cond1.evaluateCount > 1);
  }

  /**
   * Test a call to {@link ConcurrentUtils#awaitInterruptibleCondition(ConcurrentUtils.Condition, long, boolean) awaitInterruptibleCondition()} which returns {@link true}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testAwaitInteruptibleTrue() throws Exception {
    assertTrue(ConcurrentUtils.awaitInterruptibleCondition(cond1 = new Condition1(true), 500L, true));
    assertEquals(1, cond1.evaluateCount);
  }

  /**
   * Test a call to {@link ConcurrentUtils#awaitInterruptibleCondition(ConcurrentUtils.Condition, long, boolean) awaitInterruptibleCondition()} which returns {@link false}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testAwaitInteruptibleFalse() throws Exception {
    assertFalse(ConcurrentUtils.awaitInterruptibleCondition(cond1 = new Condition1(false), 500L, false));
    assertTrue(cond1.evaluateCount > 1);
  }

  /** */
  static class Condition1 implements ConcurrentUtils.Condition {
    /** */
    final boolean retValue;
    /** */
    int evaluateCount = 0;

    /**
     * @param retValue .
     */
    Condition1(final boolean retValue) {
      this.retValue = retValue;
    }

    @Override
    public boolean evaluate() {
      try {
        Thread.sleep(100L);
      } catch (Exception e) {
        print(false, false, "Exception: %s", ExceptionUtils.getStackTrace(e));
      }
      evaluateCount++;
      return retValue;
    }
  }
}
