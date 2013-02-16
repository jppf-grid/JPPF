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

package org.jppf.test.addons.mbeans;

/**
 * Interface for the diagnostics MBean.
 * @author Laurent Cohen
 */
public interface DiagnosticsMBean
{
  /**
   * The name of this mbean in a driver.
   */
  String MBEAN_NAME_DRIVER = "org.jppf:name=diagnostics,type=driver";
  /**
   * The name of this mbean in a node.
   */
  String MBEAN_NAME_NODE = "org.jppf:name=diagnostics,type=node";

  /**
   * Get the diagnostifcs info for the whole JVM.
   * @return a {@link DiagnosticsInfo} instance.
   */
  DiagnosticsInfo getDiagnosticsInfo();

  /**
   * Get the names of all live threads in the current JVM.
   * @return an arrray of thread names as strings.
   */
  String[] threadNames();

  /**
   * Perform a garbage collection. This method calls <code>System.gc()</code>.
   */
  void gc();
}
