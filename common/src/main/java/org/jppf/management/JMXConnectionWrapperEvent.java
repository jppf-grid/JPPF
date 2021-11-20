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

package org.jppf.management;

import java.util.EventObject;

/**
 * Event sent when a JMXConnectionWrapper is connected or when the connection times out.
 * @author Laurent Cohen
 */
public class JMXConnectionWrapperEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this event with the specified source.
   * @param jmxWrapper the jmx connection wrapper source of this event.
   */
  public JMXConnectionWrapperEvent(final JMXConnectionWrapper jmxWrapper) {
    super(jmxWrapper);
  }

  /**
   * Get the jmx connection wrapper source of this event.
   * @return a {@link JMXConnectionWrapper} instance.
   */
  public JMXConnectionWrapper getJMXConnectionWrapper() {
    return (JMXConnectionWrapper) getSource();
  }
}
