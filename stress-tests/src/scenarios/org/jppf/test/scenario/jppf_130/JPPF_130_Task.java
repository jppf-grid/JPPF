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

package org.jppf.test.scenario.jppf_130;

import org.jppf.node.protocol.AbstractTask;

/**
 * Task for reproducing bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-130">JPPF-130</a>.
 * @author Laurent Cohen
 */
public class JPPF_130_Task extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Number of class loader lookups to perform.
   */
  private final int nbLookups;

  /**
   * Create a new instance.
   * @param nbLookups the number of class loader lookups to perform.
   */
  public JPPF_130_Task(final int nbLookups) {
    this.nbLookups = nbLookups;
  }

  @Override
  public void run() {
    try {
      final ClassLoader cl = getClass().getClassLoader();
      for (int i = 0; i < nbLookups; i++) {
        try {
          cl.getResource("test.SomeResource" + i);
          //Class<?> clazz = cl.loadClass("test.SomeResource" + i);
        } catch (@SuppressWarnings("unused") final Exception e) {
        }
      }
      setResult("success");
    } catch (final Exception e) {
      setThrowable(e);
    }
  }
}
