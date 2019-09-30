/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.node.protocol.graph;

import java.util.List;

/**
 * Exception raised when a dependency cycle is detected in a graph of jobs.
 * @author Laurent Cohen
 */
public class JPPFJobDependencyCycleException extends JPPFDependencyCycleException {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * An ordered list of dependency ids that make the path of the detected cycle.
   */
  private final List<String> idPath;

  /**
   * Initialize this exception with a specified message.
   * @param message the message for this exception.
   * @param idPath an ordered list of dependency ids that make the path of the detected cycle.
   */
  public JPPFJobDependencyCycleException(final String message, final List<String> idPath) {
    super(message);
    this.idPath = idPath;
  }

  /**
   * Get an ordered list of dependency ids that make the path of the detected cycle.
   * @return a list of string dependency ids.
   */
  public List<String> getIdPath() {
    return idPath;
  }
}
