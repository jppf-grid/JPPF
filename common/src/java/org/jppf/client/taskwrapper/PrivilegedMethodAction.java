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
package org.jppf.client.taskwrapper;

import java.lang.reflect.Method;

/**
 * Class used to invoke a method through reflection when a security manager is present.
 */
class PrivilegedMethodAction extends AbstractPrivilegedAction<Object>
{
  /**
   * The method to invoke.
   */
  private Method method = null;
  /**
   * The object on which to invoke the method, or null for static methods.
   */
  private Object invoker = null;

  /**
   * Initialize this privileged action with the specified method, invoker object and parameters.
   * @param method the method to invoke.
   * @param invoker the object on which to invoke the method, or null for static methods.
   * @param args the parameters of the method to invoke.
   */
  public PrivilegedMethodAction(final Method method, final Object invoker, final Object[] args)
  {
    this.method = method;
    this.invoker = invoker;
    this.args = args;
  }

  /**
   * Invoke the method.
   * @return the method's return result, or null if the method has a <code>void</code> return type.
   * @see java.security.PrivilegedAction#run()
   */
  @Override
  public Object run()
  {
    Object result = null;
    try
    {
      result = method.invoke(invoker, args);
    }
    catch(Exception e)
    {
      exception = e;
    }
    return result;
  }
}
