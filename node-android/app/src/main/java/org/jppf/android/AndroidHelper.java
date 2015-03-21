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
import android.os.AsyncTask;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.NodeRunner;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * .
 */
public class AndroidHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoader.class);
  /**
   * A global application object.
   */
  public static Context context = null;
  private static final AtomicBoolean nodeLaunched = new AtomicBoolean(false);

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

  private static void launch0() {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setBoolean("jppf.node.android", true);
    config.setBoolean("jppf.node.offline", true);
    config.setBoolean("jppf.discovery.enabled", false);
    //config.setString("jppf.server.port", "10.0.2.2");
    config.setString("jppf.server.host", "192.168.1.24");
    config.setInt("jppf.server.port", 11111);
    config.setInt("jppf.processing.threads", 1);
    config.setString("jppf.node.class", "org.jppf.server.node.android.JPPFAndroidNode");
    NodeRunner.main("noLauncher");
  }
}
