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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jppf.android.activities.SettingsFragment;
import org.jppf.node.NodeRunner;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * .
 */
public class AndroidHelper {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = AndroidHelper.class.getSimpleName();
  /**
   * A global context object.
   */
  private static Context context = null;
  private static final AtomicBoolean nodeLaunched = new AtomicBoolean(false);

  /**
   * Start the node.
   * @param ctxt a global context that can be used by the node.
   */
  public static void launchNode(Context ctxt) {
    if (nodeLaunched.compareAndSet(false, true)) {
      context = ctxt;
      new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(final Void... params) {
          launch0();
          return true;
        }
      }.execute();
    }
  }

  public static Context getContext() {
    return context;
  }

  private static void launch0() {
    TypedProperties config = JPPFConfiguration.getProperties();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String[] keys = { SettingsFragment.SERVERS_KEY, SettingsFragment.THREADS_KEY };
    for (String key: keys) changeConfigFromPrefs(prefs, key);
    config.setBoolean("jppf.node.android", true);
    config.setBoolean("jppf.node.offline", true);
    config.setBoolean("jppf.discovery.enabled", false);
    config.setString("jppf.server.connection.strategy", AndroidNodeConnectionStrategy.class.getName());
    config.setString("jppf.node.class", "org.jppf.server.node.android.JPPFAndroidNode");
    NodeRunner.main("noLauncher");
  }

  public static void changeConfigFromPrefs(final SharedPreferences prefs, final String key) {
    TypedProperties config = JPPFConfiguration.getProperties();
    switch(key) {
      case SettingsFragment.SERVERS_KEY:
        String value = prefs.getString(key, "");
        Log.d(LOG_TAG, String.format("preference %s changed to %s", key, value));
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

      case SettingsFragment.THREADS_KEY:
        config.setString("jppf.processing.threads", prefs.getString(key, "1"));
        break;
    }
  }
}
