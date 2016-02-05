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
package org.jppf.android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitors the battery charge level emits notifications to registered listeners.
 * @author Laurent Cohen
 */
public class BatteryMonitor {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = BatteryMonitor.class.getSimpleName();
  /**
   * The app context.
   */
  private final Context context;
  /**
   * The last computed value of the battery charge %.
   */
  private AtomicInteger currentLevel = new AtomicInteger(-1);
  /**
   * Peridodically computes the charge level.
   */
  private Timer timer;
  /**
   * The callback to send notifications to.
   */
  private final Callback callback;
  /**
   * Threshold for the warning battery level.
   */
  private final AtomicInteger warningThreshold = new AtomicInteger(10);
  /**
   * Threshold for the critical battery level.
   */
  private final AtomicInteger criticalThreshold = new AtomicInteger(5);
  /**
   * Whether monitoring is enabled or not.
   */
  private final AtomicBoolean enabled = new AtomicBoolean(false);

  /**
   * Intiialize this monitor with the specified context, callback and thresholds and enabled state set to {@code false}.
   * @param context the app context.
   * @param callback the callback to invoke when a new charge level is detected.
   * @param warningThreshold the threshold value for warning level in % of charge.
   * @param criticalThreshold the threshold value for critical level in % of charge.
   */
  public BatteryMonitor(final Context context, final Callback callback, final int warningThreshold, final int criticalThreshold) {
    this.context = context;
    this.callback = callback;
    this.warningThreshold.set(warningThreshold);
    this.criticalThreshold.set(criticalThreshold);
    //updateChargePct();
  }

  /**
   * Get the percentage of battery charge.
   * @return the battery charge in % as a float value.
   */
  private int updateChargePct() {
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = context.registerReceiver(null, ifilter);
    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    currentLevel.set((int) (100f * level / (float) scale));
    return currentLevel.get();
  }

  /**
   * Start monitoring the battery charge level.
   */
  private void startMonitoring() {
    Log.d(LOG_TAG, "starting monitoring timer");
    timer = new Timer(getClass().getSimpleName() + " timer", true);
    timer.schedule(new TimerTask() {
      private int currentLevel = -1;

      @Override
      public void run() {
        int n = updateChargePct();
        if (n != currentLevel) {
          currentLevel = n;
          if (callback != null) callback.onChargeLevelChanged(currentLevel);
        }
      }
    }, 10L, 10_000L);
  }

  /**
   * Stop monitoring the battery charge level.
   */
  private void stopMonitoring() {
    if (timer != null) {
      Log.d(LOG_TAG, "stopping monitoring timer");
      timer.cancel();
      timer.purge();
      timer = null;
    }
  }

  /**
   * Determine whether monitoring is currently enabled.
   * @return {@code true} if monitoring is enabled, {@code false} otherwise.
   */
  public boolean isEnabled() {
    return enabled.get();
  }

  /**
   * Enable or disable monitoring.
   * @param enabled {@code true} to enable monitoring, {@code false} to disabled it.
   * @return this battery monitor.
   */
  public BatteryMonitor setEnabled(final boolean enabled) {
    boolean prev = this.enabled.getAndSet(enabled);
    if (enabled != prev) {
      if (enabled) startMonitoring();
      else stopMonitoring();
    }
    return this;
  }

  /**
   * Set the battery charge threshold for critical level.
   * @param warningThreshold the threshold value.
   */
  void setWarningThreshold(final int warningThreshold) {
    this.warningThreshold.set(warningThreshold);
  }

  /**
   * Set the battery charge threshold for critical level.
   * @param criticalThreshold the threshold value.
   */
  void setCriticalThreshold(final int criticalThreshold) {
    this.criticalThreshold.set(criticalThreshold);
  }

  /**
   * Determine whether critical level was reached.
   * @return {@code true} if critical level was reached, {@code false} otherwise.
   */
  public boolean isCritical() {
    return currentLevel.get() <= criticalThreshold.get();
  }

  /**
   * Determine whether warning level was reached.
   * @return {@code true} if warning level was reached, {@code false} otherwise.
   */
  public boolean isWarning() {
    return !isCritical() && (currentLevel.get() <= warningThreshold.get());
  }

  /**
   * Determine whether the battery charge level is ok.
   * @return {@code true} if the charge level is ok, {@code false} otherwise.
   */
  public boolean isOK() {
    return currentLevel.get() > warningThreshold.get();
  }

  /**
   * Callback interface for receiving battery level change notifications.
   */
  public interface Callback {
    void onChargeLevelChanged(final int newLevel);
  }
}
