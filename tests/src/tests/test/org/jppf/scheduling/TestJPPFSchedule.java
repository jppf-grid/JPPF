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

package test.org.jppf.scheduling;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

import org.jppf.scheduling.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit test for the <code>TypedProperties</code> class.
 * @author Laurent Cohen
 */
public class TestJPPFSchedule extends BaseTest {
  /** */
  private static final long BASE_TIMEOUT = SystemUtils.isWindows() ? 132L : 100L;
  /**
   * Max allowed error on measured timeout times.
   */
  private static final long MAX_ERROR = SystemUtils.isWindows() ? 32L : 20L;
  /** */
  private static final String STR_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  /** */
  private static final SimpleDateFormat SDF = new SimpleDateFormat(STR_FORMAT);
  /** */
  private final JPPFScheduleHandler handler = new JPPFScheduleHandler("ScheduleHandlerTest");
  /** */
  private long actualTime;
  /** */
  private boolean executed;
  /** */
  private Runnable action = () -> {
    executed = true;
    actualTime = System.currentTimeMillis() - actualTime;
  };

  /**
   * Initialization before each test.
   * @throws Exception if any error occurs.
   */
  public void setup() throws Exception {
    handler.clear(false);
    actualTime = -1L;
    executed = false;
  }

  /**
   * Test expiration of a {@link JPPFSchedule} constructed with a {@code long} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testLongDurationSchedule() throws Exception {
    testSchedule(new JPPFSchedule(BASE_TIMEOUT));
  }

  /**
   * Test expiration of a {@link JPPFSchedule} constructed with a {@code Date} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testDateSchedule() throws Exception {
    final Date date = new Date(System.currentTimeMillis() + BASE_TIMEOUT);
    final String strDate = SDF.format(date);
    testSchedule(new JPPFSchedule(strDate, STR_FORMAT));
  }

  /**
   * Test expiration of a {@link JPPFSchedule} constructed with a {@code ZonedDateTime} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testZonedDateTimeSchedule() throws Exception {
    testSchedule(new JPPFSchedule(ZonedDateTime.now().plusNanos(BASE_TIMEOUT * 1_000_000L)));
  }

  /**
   * Test expiration of a {@link JPPFSchedule} constructed with a {@code ZonedDateTime} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testDurationSchedule() throws Exception {
    testSchedule(new JPPFSchedule(Duration.ofMillis(BASE_TIMEOUT)));
  }

  /**
   * Test cancellation of a {@link JPPFSchedule} constructed with a {@code long} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testCancelLongDurationSchedule() throws Exception {
    testCancelSchedule(new JPPFSchedule(BASE_TIMEOUT));
  }

  /**
   * Test cancellation of a {@link JPPFSchedule} constructed with a {@code Date} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testCancelDateSchedule() throws Exception {
    final Date date = new Date(System.currentTimeMillis() + BASE_TIMEOUT);
    final String strDate = SDF.format(date);
    testCancelSchedule(new JPPFSchedule(strDate, STR_FORMAT));
  }

  /**
   * Test cancellation of a {@link JPPFSchedule} constructed with a {@code ZonedDateTime} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testCancelZonedDateTimeSchedule() throws Exception {
    testCancelSchedule(new JPPFSchedule(ZonedDateTime.now().plusNanos(BASE_TIMEOUT * 1_000_000L)));
  }

  /**
   * Test cancellation of a {@link JPPFSchedule} constructed with a {@code ZonedDateTime} timeout.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000L)
  public void testCancelDurationSchedule() throws Exception {
    testCancelSchedule(new JPPFSchedule(Duration.ofMillis(BASE_TIMEOUT)));
  }

  /**
   * Test that the specified schedule expires at the expected time.
   * @param schedule the schedule to test.
   * @throws Exception if any error occurs.
   */
  private void testSchedule(final JPPFSchedule schedule) throws Exception {
    final long start = System.currentTimeMillis();
    actualTime = start;
    final long excpectedDuration = schedule.toLong(start) - start;
    handler.scheduleAction("testKey", schedule, action);
    Thread.sleep(2L * excpectedDuration);
    print(false, false, "schedule = %s, measured time: %,d", schedule, actualTime);
    assertCompare(Operator.AT_LEAST, 0L, actualTime);
    assertTrue(executed);
    assertCompare(Operator.AT_LEAST, excpectedDuration - MAX_ERROR, actualTime);
    assertCompare(Operator.AT_MOST, excpectedDuration + MAX_ERROR, actualTime);
    assertFalse(handler.hasAction("testKey"));
  }

  /**
   * Test that the cancellation of the specified schedule.
   * @param schedule the schedule to test.
   * @throws Exception if any error occurs.
   */
  private void testCancelSchedule(final JPPFSchedule schedule) throws Exception {
    final long start = System.currentTimeMillis();
    actualTime = start;
    final long excpectedDuration = schedule.toLong(start) - start;
    handler.scheduleAction("testKey", schedule, action);
    Thread.sleep(excpectedDuration / 2L);
    handler.cancelAction("testKey");
    final long end = System.currentTimeMillis() - start;
    assertFalse(handler.hasAction("testKey"));
    print(false, false, "cancelled schedule = %s, measured time: %,d", schedule, end);
    assertFalse(executed);
  }
}
