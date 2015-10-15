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
package org.jppf.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jppf.android.activities.MainActivity;
import org.jppf.node.NodeRunner;
import org.jppf.android.node.JPPFAndroidNode;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.StringUtils;
import org.jppf.utils.TypedProperties;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

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
      TypedProperties config = JPPFConfiguration.getProperties();
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
      String[] keys = {
        PreferenceUtils.SERVERS_KEY, PreferenceUtils.THREADS_KEY, PreferenceUtils.KEY_STORE_LOCATION_KEY, PreferenceUtils.KEY_STORE_PASSWORD_KEY,
        PreferenceUtils.TRUST_STORE_LOCATION_KEY, PreferenceUtils.TRUST_STORE_PASSWORD_KEY, PreferenceUtils.SSL_CONTEXT_PROTOCOL_KEY,
        PreferenceUtils.SSL_ENGINE_PROTOCOL_KEY, PreferenceUtils.ENABLED_CIPHER_SUITES_KEY
      };
      for (String key: keys) changeConfigFromPrefs(prefs, key);
      config.setBoolean("jppf.node.android", true);
      config.setBoolean("jppf.node.offline", true);
      config.setInt("jppf.classloader.cache.size", 1);
      config.setBoolean("jppf.discovery.enabled", false);
      config.setString("jppf.server.connection.strategy", AndroidNodeConnectionStrategy.class.getName());
      config.setString("jppf.node.class", JPPFAndroidNode.class.getName());
      config.setString("jppf.ssl.configuration.source", SSLConfigSource.class.getName());
      config.setBoolean("jppf.ssl.enabled", true);
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
      case PreferenceUtils.SERVERS_KEY:
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

      case PreferenceUtils.THREADS_KEY:
        config.setString("jppf.processing.threads", prefs.getString(key, "1"));
        break;

      case PreferenceUtils.TRUST_STORE_LOCATION_KEY:
        setSSLProperty("jppf.ssl.truststore.source", KeyStoreUriSource.class.getName() + ' ', prefs, key);
        break;

      case PreferenceUtils.TRUST_STORE_PASSWORD_KEY:
        setSSLProperty("jppf.ssl.truststore.password", null, prefs, key);
        break;

      case PreferenceUtils.KEY_STORE_LOCATION_KEY:
        setSSLProperty("jppf.ssl.keystore.source", KeyStoreUriSource.class.getName() + ' ', prefs, key);
        break;

      case PreferenceUtils.KEY_STORE_PASSWORD_KEY:
        setSSLProperty("jppf.ssl.keystore.password", null, prefs, key);
        break;

      case PreferenceUtils.SSL_CONTEXT_PROTOCOL_KEY:
        setSSLProperty("jppf.ssl.context.protocol", null, prefs, key);
        break;

      case PreferenceUtils.SSL_ENGINE_PROTOCOL_KEY:
        setSSLProperty("jppf.ssl.protocols", null, prefs, key);
        break;

      case PreferenceUtils.ENABLED_CIPHER_SUITES_KEY:
        setSSLProperty("jppf.ssl.cipher.suites", null, prefs, key);
        break;
    }
  }

  /**
   * Set the specified SSL ocnfiguration property form the specified preference.
   * @param configKey the name of the confguration property to set.
   * @param valuePrefix a prefix for the value of the property.
   * @param prefs the shared preferences for this application.
   * @param key the preference key.
   */
  private static void setSSLProperty(final String configKey, final String valuePrefix, final SharedPreferences prefs, final String key) {
    String rep = "//data/data/org.jppf.android/files";
    String s = (valuePrefix == null) || "".equals(valuePrefix.trim()) ? "" : valuePrefix;
    String prefValue = null;
    if (PreferenceUtils.SSL_ENGINE_PROTOCOL_KEY.equals(key) || PreferenceUtils.ENABLED_CIPHER_SUITES_KEY.equals(key))
      prefValue = StringUtils.collectionToString(" ", null, null, prefs.getStringSet(key, Collections.<String>emptySet()));
    else {
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
}
