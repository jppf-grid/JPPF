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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.jppf.android.AndroidHelper;
import org.jppf.android.R;

public class MainActivity extends Activity {
  private final static String LOG_TAG = MainActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread thread, final Throwable ex) {
        System.err.print("uncaught exception in thread " + thread + ": ");
        ex.printStackTrace();
      }
    });
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    AndroidHelper.launchNode(getApplicationContext());
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
    Log.d(LOG_TAG, "onOptionsItemSelected() id = " + id);
    if (id == R.id.action_settings) {
      Log.d(LOG_TAG, "onOptionsItemSelected() showing the settings screen");
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return false;
    }
    return super.onOptionsItemSelected(item);
  }
}
