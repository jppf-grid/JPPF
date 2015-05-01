/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.utils.time;

import java.util.*;

import org.jppf.utils.time.UnixTimeMeasurement.CLIB.TIMESPEC;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

/**
 *
 * @author Laurent Cohen
 */
public class UnixTimeMeasurement implements TimeMeasurement {
  /**
   * Definition (incomplete) of the Xext library.
   */
  public interface CLIB extends Library {
    /**
     * Instance of the Xss library bindings.
     */
    CLIB INSTANCE = (CLIB) Native.loadLibrary(null, CLIB.class);

    /**
     * Access a process CPU-time clock
     * @param pid the pid of the process.
     * @param clock_id an int value sent by reference which will hold the result.
     * @return 0 upon successful completion, non-zero otherwise. 
     */
    int clock_getcpuclockid(int pid, IntByReference clock_id);

    /**
     * Get the wall clock.
     * @param clock_id id of the lcok to access.
     * @param timespec holds the results.
     * @return 0 upon successful completion, non-zero otherwise. 
     */
    int clock_gettime(int clock_id, TIMESPEC timespec);

    /**
     *
     */
    class TIMESPEC extends Structure {
      /**
       *
       */
      public int seconds;
      /**
       *
       */
      public long nanos;

      @Override
      protected List getFieldOrder() {
        return Arrays.asList(new String[] { "seconds", "nanos" });
      }
    }
  }

  /**
   * Singleton instance of this class.
   */
  private static final TimeMeasurement instance = new UnixTimeMeasurement();
  /**
   * Interval in nanos between two clock ticks.
   * Computed from a call to {@code QueryPerformanceFrequency()}.
   */
  private static int clockID = 0;
  static {
    try {
      IntByReference clock_id = new IntByReference();
      int result = CLIB.INSTANCE.clock_getcpuclockid(0, clock_id);
      clockID = clock_id.getValue();
      System.out.printf("clock_id = %,d%n", clockID);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  /**
   * Long value passed by reference to {@code QueryPerformanceCounter()}.
   */
  private TIMESPEC ref = new TIMESPEC();

  @Override
  public long getNanoTime() {
    CLIB.INSTANCE.clock_gettime(clockID, ref);
    return 1_000_000_000 * ref.seconds + ref.nanos;
  }

  /**
   * Get the latest time measure in nanoseconds.
   * @return the latest time measure in nanos.
   */
  public static long nanoTime() {
    return instance.getNanoTime();
  }

  /**
   * Main.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      // warmup
      long start = UnixTimeMeasurement.nanoTime();
      for (int i=0; i<10000; i++) {
        long tmp = UnixTimeMeasurement.nanoTime();
      }
      long elapsed = UnixTimeMeasurement.nanoTime() - start;
      System.out.printf("warmup time = %,d ns%n", elapsed);
      int nbMeasures = 11;
      long times[] = new long[nbMeasures];
      start = UnixTimeMeasurement.nanoTime();
      for (int i=0; i<nbMeasures; i++) times[i] = UnixTimeMeasurement.nanoTime();
      for (int i=1; i<nbMeasures; i++) System.out.printf("duration %2d = %,8d ns%n", i, (times[i] - times[i-1]));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
