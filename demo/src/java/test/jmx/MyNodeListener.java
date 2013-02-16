/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package test.jmx;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.event.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MyNodeListener implements NodeLifeCycleListener {

  /**
   * singleton instance for easy access from the tasks.
   */
  private static MyNodeListener instance;
  /**
   * uuid of the current job being executed, if any.
   */
  private String currentUuid = null;
  /**
   * connection to the driver's connection wrapper.
   */
  private JMXDriverConnectionWrapper driverJmx = null;

  /**
   * 
   * @return .
   */
  public static MyNodeListener getInstance() {
    return instance;
  }

  /**
   * the node should only create one instance of this listener
   */
  public MyNodeListener() {
    instance = this;
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    // store the uuid before the job starts in the node
    currentUuid = event.getJob().getUuid();
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    // reset the job uuid
    currentUuid = null;
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    // establish a connection to the driver's JMX server
    String host = "driverHost";
    int port = 11198; // use the proper port number here
    driverJmx = new JMXDriverConnectionWrapper(host, port, false);
    driverJmx.connect();
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    if ((driverJmx != null) && driverJmx.isConnected()) {
      try {
        driverJmx.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * cancel the current job, if any is running
   */
  public void cancelJob() {
    if ((currentUuid != null) && driverJmx.isConnected()) {
      try {
        driverJmx.cancelJob(currentUuid);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
