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

package org.jppf.management.diagnostics.provider;

import static org.jppf.management.diagnostics.provider.MonitoringConstants.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.utils.*;

import oshi.*;
import oshi.hardware.*;
import oshi.software.os.*;

/**
 * Instances of this class wrap a singleton {@link oshi.SystemInfo} object, which is the entry point for the
 * <a href="https://github.com/oshi/oshi">Oshi</a> API.
 * @author Laurent Cohen
 */
public final class Oshi {
  /**
   * Entry point to Oshi API.
   */
  private static SystemInfo si;
  /**
   * Provides access to hardware components.
   */
  private HardwareAbstractionLayer hal;
  /**
   * Oshi's operating system abstraction.
   */
  private OperatingSystem os;
  /**
   * Sensors include hardwore sensors to monitor temperature, fan speed, and other information.
   */
  Sensors sensors;
  /**
   * Global memory.
   */
  private GlobalMemory memory;
  /**
   * Represents the current process (i.e. JVM).
   */
  private OSProcess process;
  /**
   * Determines whether CPU termperature is available.
   */
  private final AtomicBoolean temperatureAvailable = new AtomicBoolean(true);
  /**
   * 
   */
  private PlatformEnum currentPlatform;
  /**
   * 
   */
  private CentralProcessor processor;
  /**
   * 
   */
  private String osName;
  /**
   * 
   */
  boolean swapMonitoringEnabled = true;
  /**
   * Singleton instance.
   */
  static final Oshi instance = new Oshi();
  /**
   * Whether initialization was done;
   */
  private boolean initialized;
  /**
   * The last computed values.
   */
  private TypedProperties lastValues = new TypedProperties();
  /**
   * The tiem at which the last values were computed.
   */
  private long lastTimestamp;

  /**
   * 
   */
  private Oshi() {
  }


  /**
   * Initialize the Oshi API.
   * @return this object, for method call chaining.
   */
  synchronized Oshi init() {
    if (!initialized) {
      initialized = true;
      swapMonitoringEnabled = SystemUtils.getSystemProperties().getBoolean("jppf.monitoring.data.swap.enabled", true);
      final SystemInfo si = getSystemInfo();
      this.currentPlatform = SystemInfo.getCurrentPlatformEnum();
      hal = si.getHardware();
      os = si.getOperatingSystem();
      sensors = hal.getSensors();
      memory = hal.getMemory();
      process = os.getProcess(os.getProcessId());
      processor = hal.getProcessor();
      osName = os.getFamily() + " " + os.getVersion().getVersion();
    }
    return this;
  }

  /**
   * @return the values.
   */
  synchronized TypedProperties getValues() {
    final long currentTime = System.currentTimeMillis();
    if ((lastTimestamp <= 0L) || (currentTime - lastTimestamp > 1000L)) {
      lastTimestamp = currentTime;
      lastValues = new TypedProperties();
      lastValues.setString(OS_NAME, osName);
      double temp = -1d;
      if (temperatureAvailable.get()) {
        temp = sensors.getCpuTemperature();
        if (temp <= 0d) {
          temperatureAvailable.set(false);
          temp = -1d;
        }
      }
      lastValues.setDouble(CPU_TEMPERATURE, temp);
      final double total = memory.getTotal();
      final double available = memory.getAvailable();
      lastValues.setDouble(RAM_USAGE_MB, (total - available) / MB);
      lastValues.setDouble(RAM_USAGE_RATIO, 100d * (total - available) / total);
      if (swapMonitoringEnabled) {
        // swap info retieval is the one that consumes the most cpu.
        final double swapTotal = memory.getSwapTotal();
        final double swapUsed = memory.getSwapUsed();
        lastValues.setDouble(SWAP_USAGE_MB, swapUsed / MB);
        lastValues.setDouble(SWAP_USAGE_RATIO, 100d * swapUsed / swapTotal);
      }
      lastValues.setDouble(PROCESS_RESIDENT_SET_SIZE, (double) process.getResidentSetSize() / MB);
      lastValues.setDouble(PROCESS_VIRTUAL_SIZE, (double) process.getVirtualSize() / MB);
      lastValues.setDouble(SYSTEM_CPU_LOAD, 100d * processor.getSystemCpuLoadBetweenTicks());
    }
    return lastValues;
  }

  /**
   * @return the entry point to the Oshi API.
   */
  public static synchronized SystemInfo getSystemInfo() {
    if (si == null) si = new SystemInfo();
    return si;
  }

  /**
   * Get the name of the current OS family (one of "WINDOWS", "LINUX", "MACOSX", "SOLARIS", "FREEBSD", "UNKNOWN").
   * @return  the name of the current platform (OS family).
   */
  public String getCurrentPlatformName() {
    return currentPlatform == null ? null : currentPlatform.name();
  }
}
