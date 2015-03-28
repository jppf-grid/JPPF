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
package org.jppf.android.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import org.jppf.android.AndroidHelper;
import org.jppf.android.R;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

/**
 * This class manages the settings screen and updates the JPPF configuration whenever a preference changes.
 * @author Laurent Cohen
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = SettingsFragment.class.getSimpleName();
  /**
   * The key name for the server connections preference.
   */
  public static final String SERVERS_KEY = "pref_servers";
  /**
   * The key name for the processing threads preference.
   */
  public static final String THREADS_KEY = "pref_threads";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override
  public void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
    AndroidHelper.changeConfigFromPrefs(prefs, key);
  }
}
