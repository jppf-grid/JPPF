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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jppf.android.activities.MainActivity;
import org.jppf.android.node.AndroidSerializationExceptionHook;
import org.jppf.android.node.JPPFAndroidNode;
import org.jppf.node.NodeRunner;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.BooleanProperty;
import org.jppf.utils.configuration.IntProperty;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.configuration.JPPFProperty;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jppf.android.PreferenceUtils.BATTERY_MONITORING_CRITICAL_KEY;
import static org.jppf.android.PreferenceUtils.BATTERY_MONITORING_ENABLED_KEY;
import static org.jppf.android.PreferenceUtils.BATTERY_MONITORING_WARNING_KEY;
import static org.jppf.android.PreferenceUtils.ENABLED_CIPHER_SUITES_KEY;
import static org.jppf.android.PreferenceUtils.KEY_STORE_LOCATION_KEY;
import static org.jppf.android.PreferenceUtils.KEY_STORE_PASSWORD_KEY;
import static org.jppf.android.PreferenceUtils.SERVERS_KEY;
import static org.jppf.android.PreferenceUtils.SSL_CONTEXT_PROTOCOL_KEY;
import static org.jppf.android.PreferenceUtils.SSL_ENGINE_PROTOCOL_KEY;
import static org.jppf.android.PreferenceUtils.THREADS_KEY;
import static org.jppf.android.PreferenceUtils.TRUST_STORE_LOCATION_KEY;
import static org.jppf.android.PreferenceUtils.TRUST_STORE_PASSWORD_KEY;

/**
 * Utility methods for launching a node and managing its configuration.
 */
public class AndroidHelper {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = AndroidHelper.class.getSimpleName();
  /**
   * A global activity object.
   */
  private static Activity activity = null;
  /**
   * Property name for batetry monitoring enabled flag.
   */
  private static final JPPFProperty<Boolean> BATTERY_MONITORING_ENABLED = new BooleanProperty("jppf.android.battery.monitoring.enabled", true);
  /**
   * Property name for the warning level threshold.
   */
  private static final JPPFProperty<Integer> BATTERY_WARNING_THRESHOLD = new IntProperty("jppf.android.battery.warning.threshold", 10);
  /**
   * Property name for the critical level threshold.
   */
  private static final JPPFProperty<Integer> BATTERY_CRITICAL_THRESHOLD = new IntProperty("jppf.android.battery.critical.threshold", 5);
  /**
   *Determines whther the node is currently running.
   */
  private static final AtomicBoolean nodeLaunched = new AtomicBoolean(false);
  /**
   * The SSL/TLS configuration.
   */
  private static final TypedProperties sslConfig = new TypedProperties();
  /**
   * The default handler for uncaught exceptions.
   */
  private static final Thread.UncaughtExceptionHandler ueh = new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
      Log.e(LOG_TAG, "uncaught exception in thread " + thread + " : ", ex);
    }
  };
  /**
   * Monitors the battery charge level.
   */
  private static BatteryMonitor monitor;

  /**
   * Instantiation of this class is not permitted.
   */
  private AndroidHelper() {
  }

  /**
   * Get the activity used by the node.
   * @return a {@link Context} object.
   */
  public static Activity getActivity() {
    return activity;
  }

  /**
   * Get the SSL/TLS configuration.
   * @return the ocnfiguration as a {@link TypedProperties) object.
   */
  public static TypedProperties getSSLConfig() {
    return sslConfig;
  }

  /**
   * Start the node in a separate thread.
   * @param activity a global activity that can be used by the node.
   */
  public static void launchNode(MainActivity activity) {
    if (nodeLaunched.compareAndSet(false, true)) {
      AndroidHelper.activity = activity;
      initBatteryMonitor();
      new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(final Void... params) {
          launch0();
          return true;
        }
      }.execute();
    }
  }

  /**
   * Set the configuration and start the node in the current thread.
   */
  private static void launch0() {
    try {
      setUncaughtExceptionHandler();
      changeConfigFromPrefs();
      NodeRunner.main("noLauncher");
    } catch(Exception e) {
      Log.e(LOG_TAG, "exception in launch0() : ", e);
    }
  }

  /**
   * Set the node configuration from the settings.
   */
  public static void changeConfigFromPrefs() {
    try {
      JPPFConfiguration.getProperties()
        .set(JPPFProperties.NODE_ANDROID, true)
        .set(JPPFProperties.NODE_OFFLINE, true)
        .set(JPPFProperties.CLASSLOADER_CACHE_SIZE, 1)
        .set(JPPFProperties.DISCOVERY_ENABLED, false)
        .set(JPPFProperties.SERVER_CONNECTION_STRATEGY, AndroidNodeConnectionStrategy.class.getName())
        .set(JPPFProperties.NODE_CLASS, JPPFAndroidNode.class.getName())
        .set(JPPFProperties.SSL_CONFIGURATION_SOURCE, SSLConfigSource.class.getName())
        .set(JPPFProperties.SSL_ENABLED, true)
        .set(JPPFProperties.RESOURCE_CACHE_STORAGE, "memory")
        .set(JPPFProperties.SERIALIZATION_EXCEPTION_HOOK, AndroidSerializationExceptionHook.class.getName());
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
      for (String key: PreferenceUtils.PREF_KEYS) changeConfigFromPrefs(prefs, key);
    } catch(Exception e) {
      Log.e(LOG_TAG, "exception in changeConfigFromPrefs() : ", e);
    }
  }

  /**
   * Change the configuration based on the specified preference.
   * @param prefs the shared preferences for the node application.
   * @param key the preference key from which to change a configuration property.
   */
  public static void changeConfigFromPrefs(final SharedPreferences prefs, final String key) {
    TypedProperties config = JPPFConfiguration.getProperties();
    switch(key) {
      case SERVERS_KEY:
        String value = prefs.getString(key, "");
        Log.v(LOG_TAG, String.format("preference %s changed to %s", key, value));
        String[] servers = value.split("\\s");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String s: servers) {
          if (count > 0) sb.append(" ");
          sb.append(s);
          count++;
        }
        config.setString("jppf.node.android.connections", sb.toString());
        break;

      case THREADS_KEY:
        int n = 1;
        try {
          n = prefs.getInt(key, 1);
        } catch(Exception e) {
          try {
            String s = prefs.getString(key, "1");
            n = Integer.valueOf(s);
          } catch(Exception ignore) {
          }
        }
        config.set(JPPFProperties.PROCESSING_THREADS, n);
        break;

      case BATTERY_MONITORING_ENABLED_KEY:
        boolean enabled = prefs.getBoolean(key, true);
        config.set(BATTERY_MONITORING_ENABLED, enabled);
        if (monitor != null) monitor.setEnabled(enabled);
        break;

      case BATTERY_MONITORING_WARNING_KEY:
        int intValue = prefs.getInt(key, 10);
        config.set(BATTERY_WARNING_THRESHOLD, intValue);
        if (monitor != null) monitor.setWarningThreshold(intValue);
        break;

      case BATTERY_MONITORING_CRITICAL_KEY:
        intValue = prefs.getInt(key, 5);
        config.set(BATTERY_CRITICAL_THRESHOLD, intValue);
        if (monitor != null) monitor.setCriticalThreshold(intValue);
        break;

      case TRUST_STORE_LOCATION_KEY:
        setSSLProperty(JPPFProperties.SSL_TRUSTSTORE_SOURCE, KeyStoreUriSource.class.getName() + ' ', prefs, key);
        break;

      case TRUST_STORE_PASSWORD_KEY:
        setSSLProperty(JPPFProperties.SSL_TRUSTSTORE_PASSWORD, null, prefs, key);
        break;

      case KEY_STORE_LOCATION_KEY:
        setSSLProperty(JPPFProperties.SSL_KEYSTORE_SOURCE, KeyStoreUriSource.class.getName() + ' ', prefs, key);
        break;

      case KEY_STORE_PASSWORD_KEY:
        setSSLProperty(JPPFProperties.SSL_KEYSTORE_PASSWORD, null, prefs, key);
        break;

      case SSL_CONTEXT_PROTOCOL_KEY:
        setSSLProperty(JPPFProperties.SSL_CONTEXT_PROTOCOL, null, prefs, key);
        break;

      case SSL_ENGINE_PROTOCOL_KEY:
        setSSLProperty(JPPFProperties.SSL_PROTOCOLS, null, prefs, key);
        break;

      case ENABLED_CIPHER_SUITES_KEY:
        setSSLProperty(JPPFProperties.SSL_CIPHER_SUITES, null, prefs, key);
        break;
    }
  }

  /**
   * Set the specified SSL ocnfiguration property form the specified preference.
   * @param prop the confguration property to set.
   * @param valuePrefix a prefix for the value of the property.
   * @param prefs the shared preferences for this application.
   * @param key the preference key.
   */
  private static void setSSLProperty(final JPPFProperty<?> prop, final String valuePrefix, final SharedPreferences prefs, final String key) {
    String configKey = prop.getName();
    String rep = "//data/data/org.jppf.android/files";
    String s = (valuePrefix == null) || "".equals(valuePrefix.trim()) ? "" : valuePrefix;
    String prefValue = null;
    if (SSL_ENGINE_PROTOCOL_KEY.equals(key) || ENABLED_CIPHER_SUITES_KEY.equals(key)) {
      int count = 0;
      StringBuilder sb = new StringBuilder();
      for (String val: prefs.getStringSet(key, Collections.<String>emptySet())) {
        if (count > 0) sb.append(" ");
        sb.append(val);
        count++;
      }
      prefValue = sb.toString();
    } else {
      prefValue = prefs.getString(key, "");
      if (prefValue.contains(rep)) {
        prefValue = prefValue.replace(rep, "");
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, prefValue);
        edit.commit();
      }
    }
    String value = s + prefValue;
    sslConfig.setString(configKey, value);
    Log.v(LOG_TAG, "set SSL property: " + configKey + " = " + value);
  }

  public static void setUncaughtExceptionHandler() {
    Thread.currentThread().setUncaughtExceptionHandler(ueh);
  }

  public static void setDefaultUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(ueh);
  }

  public static boolean isNodeLaunched() {
    return nodeLaunched.get();
  }

  public static BatteryMonitor getBatteryMonitor() {
    return monitor;
  }

  /**
   * Initialize the object that monitors the battery charge level.
   */
  private static void initBatteryMonitor() {
    TypedProperties config = JPPFConfiguration.getProperties();
    monitor = new BatteryMonitor(getActivity(), new BatteryMonitor.Callback() {
      @Override
      public void onChargeLevelChanged(final int newLevel) {
        JPPFNode node = (JPPFNode) NodeRunner.getNode();
        if (monitor.isCritical()) {
          Log.d(LOG_TAG, "reached critical level " + newLevel + "%, app will close !!!");
          AndroidHelper.getActivity().finishAffinity();
          System.exit(0);
        } else if (monitor.isWarning()) {
          Log.d(LOG_TAG, "reached warning level " + newLevel + "%, no longer accepting jobs");
          if (node != null) {
            node.setSuspended(true);
            if (node.isReading()) {
              try {
                node.closeDataChannel();
              } catch(Exception ignore) {
              }
            }
          }
        } else {
          if ((node != null) && node.isSuspended()) {
            Log.d(LOG_TAG, "reached green level " + newLevel + "%, accepting jobs again");
            node.setSuspended(false);
          }
        }
      }
    }, config.get(BATTERY_WARNING_THRESHOLD), config.get(BATTERY_CRITICAL_THRESHOLD)).setEnabled(config.get(BATTERY_MONITORING_ENABLED));
  }
}
