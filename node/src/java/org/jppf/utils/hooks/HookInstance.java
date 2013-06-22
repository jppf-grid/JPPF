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

package org.jppf.utils.hooks;

import java.lang.reflect.Method;

/**
 * Instance of a {@link Hook}.
 * @param <E> the type of th ehook's interface.
 * @author Laurent Cohen
 */
public class HookInstance<E>
{
  /**
   * The method to invoke on the hook instance.
   */
  private final Method method;
  /**
   * The hook instance on which to invoke the method.
   */
  private final E instance;

  /**
   * Initialize this hook with the specified method and instance.
   * @param method the method to invoke on the hook instance.
   * @param instance the hook instance on which to invoke the method.
   */
  public HookInstance(final Method method, final E instance)
  {
    this.method = method;
    this.instance = instance;
  }

  /**
   * Invoke the method on the hook instance, using the specified parameters.
   * @param parameters the parmeters to use in the method invocation.
   * @return the return value of th emethod, or <code>null</code> if the method does not return anything.
   * @throws Exception if any error occurs while invoking the method.
   */
  public Object invoke(final Object...parameters) throws Exception
  {
    return method.invoke(instance, parameters);
  }
}
