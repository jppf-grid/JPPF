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

package org.jppf.ssl;

import java.util.concurrent.Callable;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class PasswordSource implements Callable<char[]>
{
  /**
   * Optional arguments that may be specified in the configuration.
   */
  protected final String[] args;

  /**
   * Build this secure store source with the spcified arguments.
   * @param args optional arguments that may be specified in the configuration.
   */
  protected PasswordSource(final String...args)
  {
    this.args = args;
  }
}
