/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.node.initialization;

import org.jppf.utils.UnmodifiableTypedProperties;

/**
 * Interface for custom discovery of the driver to connect to.
 * <p>By definition, an initialization hook must be deployed in the node's classpath,
 * since it must be known before the node connects to the driver, which means that
 * it cannot be downloaded from the driver.
 * @author Laurent Cohen
 */
public interface InitializationHook
{
  /**
   * This method is called each time the node is about to attempt to connect to a driver.
   * It can be used to modify the current configuration via {@link org.jppf.utils.JPPFConfiguration#getProperties()}.
   * @param initialConfiguration the un-modified configuration properties of the node at startup time.
   */
  void initializing(UnmodifiableTypedProperties initialConfiguration);
}
