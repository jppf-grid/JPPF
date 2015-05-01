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

import com.sun.jna.*;
import com.sun.jna.ptr.LongByReference;

/**
 *
 * @author Laurent Cohen
 */
public class WindowsTimeMeasurement implements TimeMeasurement {
  /**
   * Definition (incomplete) of the Xext library.
   */
  public interface Kernel32 extends Library {
    /**
     * Instance of the Xss library bindings.
     */
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);

    /**
     * Query the number of clock ticks per second.
     * @param count a long value sent by reference which will hold the result.
     * @return {@code true} if no error, {@code false} otherwise.
     */
    boolean QueryPerformanceFrequency(LongByReference count);

    /**
     * Query the number of clock ticks since the start.
     * @param count a long value sent by reference which will hold the result.
     * @return {@code true} if no error, {@code false} otherwise.
     */
    boolean QueryPerformanceCounter(LongByReference count);
  }

  /**
   * Interval in nanos between two clock ticks.
   * Computed from a call to {@code QueryPerformanceFrequency()}.
   */
  private static long tickInterval = -1L;
  static {
    try {
      LongByReference ref = new LongByReference();
      Kernel32.INSTANCE.QueryPerformanceFrequency(ref);
      tickInterval = 1_000_000_000L / ref.getValue();
      System.out.printf("raw performance frequency = %,d, tick interval = %d ns%n", ref.getValue(), tickInterval);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
  /**
   * Long value passed by reference to {@code QueryPerformanceCounter()}.
   */
  private LongByReference ref = new LongByReference();

  @Override
  public long nanoTime() {
    Kernel32.INSTANCE.QueryPerformanceCounter(ref);
    return tickInterval * ref.getValue();
  }

  @Override
  public void warmUp() {
    long start = nanoTime();
    for (int i=0; i<20_000; i++) {
      long tmp = nanoTime();
    }
    long elapsed = nanoTime() - start;
    System.out.printf("warmup time = %,d ns%n", elapsed);
  }

  /**
   * Main.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      TimeMeasurement tm = new WindowsTimeMeasurement();
      tm.warmUp();
      int nbMeasures = 11;
      long times[] = new long[nbMeasures];
      long start = tm.nanoTime();
      for (int i=0; i<nbMeasures; i++) times[i] = tm.nanoTime();
      for (int i=1; i<nbMeasures; i++) System.out.printf("duration %2d = %,8d ns%n", i, (times[i] - times[i-1]));
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
