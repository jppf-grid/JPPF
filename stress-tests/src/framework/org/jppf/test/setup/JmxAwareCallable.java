/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.test.setup;

import java.util.concurrent.Callable;

import org.jppf.management.JMXConnectionWrapper;

/**
 * A {@link Callable} implementation which holds a reference to a {@link JMXConnectionWrapper}.
 * @param <V> the type of results returned by this callable.
 * @author Laurent Cohen
 */
public abstract class JmxAwareCallable<V> implements Callable<JMXResult<V>>
{
  /**
   * The JMX connection wrapper.
   */
  protected JMXConnectionWrapper jmx = null;

  /**
   * Get the JMX connection wrapper.
   * @return a {@link JMXConnectionWrapper} instance.
   */
  public JMXConnectionWrapper getJmx()
  {
    return jmx;
  }

  /**
   * Set the JMX connection wrapper.
   * @param jmx a {@link JMXConnectionWrapper} instance.
   */
  public void setJmx(final JMXConnectionWrapper jmx)
  {
    this.jmx = jmx;
  }
}
