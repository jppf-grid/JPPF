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

package test.org.jppf.utils;

import static org.junit.Assert.*;

import org.jppf.utils.Range;
import org.junit.Test;

/**
 * Unit tests for the <code>Range</code> class.
 * @author Laurent Cohen
 */
public class TestRange
{
  /**
   * Test of <code>Range.isValueInRange()</code> methods.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testIsValueInRange() throws Exception
  {
    Range<Integer> range = new Range<Integer>(10, 23);
    assertTrue(range.isValueInRange(10));
    assertFalse(range.isValueInRange(10, false));
    assertTrue(range.isValueInRange(23));
    assertFalse(range.isValueInRange(23, false));
    assertTrue(range.isValueInRange(17));
    assertTrue(range.isValueInRange(17, false));
    assertFalse(range.isValueInRange(-5));
    assertFalse(range.isValueInRange(-5, false));
    assertFalse(range.isValueInRange(55));
    assertFalse(range.isValueInRange(55, false));
  }

  /**
   * Test of <code>Range.intersects()</code> methods.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testIntersects() throws Exception
  {
    Range<Double> r1 = new Range<Double>(10d, 23d);
    Range<Double> other = new Range<Double>(-10d, 2.5d);
    assertFalse(other.intersects(r1));
    assertFalse(other.intersects(r1, false));
    other = new Range<Double>(-10d, 11.23d);
    assertTrue(other.intersects(r1));
    assertTrue(other.intersects(r1, false));
    other = new Range<Double>(-10d, 10d);
    assertTrue(other.intersects(r1));
    assertFalse(other.intersects(r1, false));
    other = new Range<Double>(23d, 40d);
    assertTrue(other.intersects(r1));
    assertFalse(other.intersects(r1, false));
    other = new Range<Double>(11d, 15d);
    assertTrue(other.intersects(r1));
    assertTrue(other.intersects(r1, false));
    other = new Range<Double>(-11d, 40d);
    assertTrue(other.intersects(r1));
    assertTrue(other.intersects(r1, false));
  }

  /**
   * Test of <code>Range.includes()</code> methods.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testIncludes() throws Exception
  {
    Range<Double> r1 = new Range<Double>(10d, 23d);
    Range<Double> other = new Range<Double>(11d, 22d);
    assertFalse(other.includes(r1));
    assertTrue(r1.includes(other));
    other = new Range<Double>(-11d, 22d);
    assertFalse(other.includes(r1));
    assertFalse(r1.includes(other));
    other = new Range<Double>(11d, 25d);
    assertFalse(other.includes(r1));
    assertFalse(r1.includes(other));
  }

  /**
   * Test of <code>Range.merge()</code> methods.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testMerge() throws Exception
  {
    Range<Integer> r1 = new Range<Integer>(10, 23);
    assertEquals(new Range<Integer>(10, 23), r1.merge(new Range<Integer>(10, 23)));
    assertEquals(new Range<Integer>(10, 23), r1.merge(new Range<Integer>(12, 21)));
    assertEquals(new Range<Integer>(-5, 23), r1.merge(new Range<Integer>(-5, 5)));
    assertEquals(new Range<Integer>(10, 40), r1.merge(new Range<Integer>(30, 40)));
    assertEquals(new Range<Integer>(-5, 23), r1.merge(new Range<Integer>(-5, 15)));
    assertEquals(new Range<Integer>(10, 40), r1.merge(new Range<Integer>(20, 40)));
  }
  /**
   * Test of <code>Range.intersection()</code> methods.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testIntersection() throws Exception
  {
    Range<Integer> r1 = new Range<Integer>(10, 23);
    assertEquals(new Range<Integer>(10, 23), r1.intersection(new Range<Integer>(10, 23)));
    assertEquals(new Range<Integer>(12, 21), r1.intersection(new Range<Integer>(12, 21)));
    assertEquals(new Range<Integer>(10, 23), r1.intersection(new Range<Integer>(-51, 51)));
    assertNull(r1.intersection(new Range<Integer>(-5, 5)));
    assertNull(r1.intersection(new Range<Integer>(30, 40)));
    assertEquals(new Range<Integer>(10, 15), r1.intersection(new Range<Integer>(-5, 15)));
    assertEquals(new Range<Integer>(20, 23), r1.intersection(new Range<Integer>(20, 40)));
  }
}
