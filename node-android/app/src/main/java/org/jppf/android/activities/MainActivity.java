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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.jppf.android.AndroidHelper;
import org.jppf.android.R;
import org.jppf.android.node.DelegatingNodeEventHandler;
import org.jppf.android.node.JPPFAndroidNode;

/**
 * Main activity for this app. Provides a main screen updated with the node activity and a settings button to configure the node.
 */
public class MainActivity extends Activity {
  /**
   * Log tag for this class.
   */
  private final static String LOG_TAG = MainActivity.class.getSimpleName();

  public MainActivity() {
    Log.v(LOG_TAG, "in constructor MainActivity()");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidHelper.setUncaughtExceptionHandler();
    AndroidHelper.setDefaultUncaughtExceptionHandler();
    setContentView(R.layout.activity_main);
    DelegatingNodeEventHandler handler = JPPFAndroidNode.getHandler();
    Log.v(LOG_TAG, String.format("onCreate(), thread=%s, handler=%s", Thread.currentThread(), handler));
    if (handler != null) {
      handler.setActivity(this);
      handler.resetUI();
    }
    else AndroidHelper.launchNode(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.v(LOG_TAG, "onDestroy(), thread = " + Thread.currentThread());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    //Log.v(LOG_TAG, "onOptionsItemSelected() id = " + id);
    if (id == R.id.action_settings) {
      //Log.v(LOG_TAG, "onOptionsItemSelected() showing the settings screen");
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return false;
    }
    return super.onOptionsItemSelected(item);
  }
}
