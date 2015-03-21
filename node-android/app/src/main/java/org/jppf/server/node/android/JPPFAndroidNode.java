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
package org.jppf.server.node.android;

import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.server.node.remote.AbstractRemoteNode;

/**
 * Implementation of a JPPF node for the Android platform.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JPPFAndroidNode extends AbstractRemoteNode {
  /**
   * Initialize this node.
   * @param connectionInfo the server connection information.
   */
  public JPPFAndroidNode(DriverConnectionInfo connectionInfo) {
    super(connectionInfo);
  }

  @Override
  protected void initClassLoaderManager() {
    this.classLoaderManager = new AndroidClassLoaderManager();
  }

  @Override
  public boolean isAndroid() {
    return true;
  }
}
