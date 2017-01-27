/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
package org.jppf.android.node;

import org.jppf.android.AndroidHelper;
import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.server.node.remote.AbstractRemoteNode;

/**
 * Implementation of a JPPF node for the Android platform.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JPPFAndroidNode extends AbstractRemoteNode {
  /**
   * Tag used for logging.
   */
  private final static String LOG_TAG = JPPFAndroidNode.class.getSimpleName();

  /**
   * Delegates node events to another, dynamically loaded event handler, if any.
   */
  private static DelegatingNodeEventHandler handler = null;
  /**
   *
   */
  private boolean alreadyAdded = false;

  /**
   * Initialize this node.
   * @param connectionInfo the server connection information.
   */
  public JPPFAndroidNode(DriverConnectionInfo connectionInfo) {
    super(connectionInfo);
  }

  @Override
  public void initDataChannel() throws Exception {
    if (handler == null) {
      handler = new DelegatingNodeEventHandler(AndroidHelper.getActivity());
    }
    if (!alreadyAdded) {
      lifeCycleEventHandler.addNodeLifeCycleListener(handler);
      getExecutionManager().getTaskNotificationDispatcher().addTaskExecutionListener(handler);
      alreadyAdded = true;
    }
    setSuspended(AndroidHelper.getBatteryMonitor().isWarning());
    super.initDataChannel();
  }

  @Override
  protected void initClassLoaderManager() {
    this.classLoaderManager = new AndroidClassLoaderManager();
  }

  @Override
  public boolean isAndroid() {
    return true;
  }

  /**
   * Get the object that delegates node events to another, dynamically loaded event handler, if any.
   * @return a {@link DelegatingNodeEventHandler} instance, or {@code null} if hasn't yet been created.
   */
  public static DelegatingNodeEventHandler getHandler() {
    return handler;
  }
}
