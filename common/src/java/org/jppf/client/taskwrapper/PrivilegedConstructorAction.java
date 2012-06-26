/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.lang.reflect.Constructor;

/**
 * Class used to invoke a constructor through reflection when a security manager is present.
 * @exclude
 */
class PrivilegedConstructorAction extends AbstractPrivilegedAction<Object>
{
  /**
   * The method to invoke.
   */
  private Constructor constructor = null;

  /**
   * Initialize this privileged action with the specified constructor and parameters.
   * @param constructor the constructor to invoke.
   * @param args the parameters of the constructor to invoke.
   */
  public PrivilegedConstructorAction(final Constructor constructor, final Object[] args)
  {
    this.constructor = constructor;
    this.args = args;
  }

  /**
   * Invoke the constructor.
   * @return the object constructed through the constructor's invocation.
   * @see java.security.PrivilegedAction#run()
   */
  @Override
  public Object run()
  {
    Object result = null;
    try
    {
      result = constructor.newInstance(args);
    }
    catch(Exception e)
    {
      exception = e;
    }
    return result;
  }
}
