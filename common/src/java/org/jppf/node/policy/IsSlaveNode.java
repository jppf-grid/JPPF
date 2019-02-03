/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.policy;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.PropertiesCollection;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * An execution policy predicates that determines whether a node is a master node.
 * @author Laurent Cohen
 */
public class IsSlaveNode extends ExecutionPolicy {
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    return ((JPPFSystemInformation) info).getJppf().get(JPPFProperties.PROVISIONING_SLAVE);
  }

  @Override
  public String toString(final int n) {
    return new StringBuilder(indent(n)).append("<IsSlaveNode/>\n").toString();
  }

  @Override
  public String toXML() {
    return super.toXML();
  }
}
