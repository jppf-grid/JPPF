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

package test.org.jppf.client;

import static org.junit.Assert.*;

import org.jppf.client.Operator;
import org.junit.Test;

/**
 * Tests for the {@link Operator} class.
 * @author Laurent Cohen
 */
public class TestOperator {
  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testEqual() throws Exception {
    assertTrue(Operator.EQUAL.evaluate(2, 2));
    assertFalse(Operator.EQUAL.evaluate(1, 2));
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotEqual() throws Exception {
    assertFalse(Operator.NOT_EQUAL.evaluate(2, 2));
    assertTrue(Operator.NOT_EQUAL.evaluate(1, 2));
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testAtLeast() throws Exception {
    assertTrue(Operator.AT_LEAST.evaluate(2, 2));
    assertTrue(Operator.AT_LEAST.evaluate(3, 2));
    assertFalse(Operator.AT_LEAST.evaluate(1, 2));
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testAtMost() throws Exception {
    assertTrue(Operator.AT_MOST.evaluate(1, 2));
    assertTrue(Operator.AT_MOST.evaluate(2, 2));
    assertFalse(Operator.AT_MOST.evaluate(3, 2));
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMoreThan() throws Exception {
    assertTrue(Operator.MORE_THAN.evaluate(3, 2));
    assertFalse(Operator.MORE_THAN.evaluate(2, 2));
    assertFalse(Operator.MORE_THAN.evaluate(1, 2));
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testLessThan() throws Exception {
    assertTrue(Operator.LESS_THAN.evaluate(1, 2));
    assertFalse(Operator.LESS_THAN.evaluate(2, 2));
    assertFalse(Operator.LESS_THAN.evaluate(3, 2));
  }
}
