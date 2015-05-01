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

import org.jppf.utils.*;
import org.slf4j.*;

import com.sun.jna.Platform;

/**
 * Factory class for creating TimeMeasurmeentInstances.
 * @author Laurent Cohen
 */
public class TimeMeasurementFactory {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TimeMeasurementFactory.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether high-resolution native clock is enabled.
   */
  private static final boolean NATIVE_CLOCK_ENABLED = JPPFConfiguration.getProperties().getBoolean("jppf.native.clock.enabled", true);

  /**
   * Create a {@link TimeMeasurement} instance for the urrent platform.
   * @return a {@link TimeMeasurement} instance.
   */
  public static TimeMeasurement createTimeMeasurement() {
    TimeMeasurement result = null;
    String cls = TimeMeasurement.class.getPackage().getName() + ".";
    try {
      if (Platform.isWindows()) cls += "WindowsTimeMeasurement";
      else cls += "UnixTimeMeasurement";
      Class<?> c = Class.forName(cls);
      result = (TimeMeasurement) c.newInstance();
    } catch (Throwable t) {
      log.warn("could not create native time measurement, returning Java instance instead", t);
    }
    result.warmUp();
    return result;
  }

  /**
   * Main.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      TimeMeasurement ptm = TimeMeasurementFactory.createTimeMeasurement();
      TimeMeasurement jtm = new JavaTimeMeasurement();
      jtm.warmUp();
      int nbMeasures = 11;
      long times[] = new long[nbMeasures];
      TimeMeasurement[] measurements = { ptm, jtm };
      for (TimeMeasurement tm: measurements) {
        System.out.printf("***** for '%s' *****%n", tm.getClass().getSimpleName());
        for (int i=0; i<nbMeasures; i++) times[i] = tm.nanoTime();
        for (int i=1; i<nbMeasures; i++) System.out.printf("duration %2d = %,8d ns%n", i, (times[i] - times[i-1]));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
