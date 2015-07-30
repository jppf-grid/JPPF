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

package org.jppf.client.monitoring.jobs;

import org.jppf.client.monitoring.AbstractComponent;

/**
 * Base superclass for components of a JPPF job hierarchy.
 * @author Laurent Cohen
 * @since 5.1
 */
public abstract class AbstractJobComponent extends AbstractComponent<AbstractJobComponent> {
  /**
   * Initialize this component with the specified uuid.
   * @param uuid the uuid assigned to this component.
   */
  AbstractJobComponent(final String uuid) {
    super(uuid);
  }
}
