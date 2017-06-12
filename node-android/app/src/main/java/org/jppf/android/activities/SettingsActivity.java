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
package org.jppf.android.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity that displays the node configuration settings.
 */
public class SettingsActivity extends Activity {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = SettingsActivity.class.getSimpleName();
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(LOG_TAG, "in onCreate()");
    // Display the fragment as the main content.
    FragmentManager fragmentManager = getFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    SettingsFragment fragment = new SettingsFragment();
    transaction.replace(android.R.id.content, fragment);
    transaction.commit();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.v(LOG_TAG, "in onDestroy()");
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.v(LOG_TAG, "in onPause()");
  }
}
