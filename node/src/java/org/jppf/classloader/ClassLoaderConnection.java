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

package org.jppf.classloader;

import java.util.*;

import org.jppf.node.NodeConnection;

/**
 * Instances of this class represent the connection between a node's class loader and the driver.
 * @param <C> the type of communication channel used by this connection.
 * @author Laurent Cohen
 * @exclude
 */
public interface ClassLoaderConnection<C> extends NodeConnection<C>
{
  /**
   * Load the specified class from a driver connection.
   * @param map contains the necessary resource request data.
   * @param dynamic true for a client class loader, false otherwise.
   * @param requestUuid identifies for which job the request is made.
   * @param uuidPath identifies the path to the driver or client that wil lookup and send back the requested resource.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws Exception if the connection was lost and could not be reestablished.
   */
  JPPFResourceWrapper loadResource(final Map<String, Object> map, final boolean dynamic, final String requestUuid, final List<String> uuidPath) throws Exception;
}

