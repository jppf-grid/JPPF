/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
 * A password source which uses a plain text string
 * @author Laurent Cohen
 */
public class PlainTextPassword implements Callable<char[]>
{
  /**
   * Optional arguments that may be specified in the configuration.
   */
  private final String[] args;

  /**
   * Initialize this password source with a plain text password.
   * @param args the first argument represents a plain text password.
   * @throws Exception if any error occurs.
   */
  public PlainTextPassword(final String... args) throws Exception
  {
    this.args = args;
    if ((args == null) || (args.length == 0)) throw new SSLConfigurationException("missing plain text password");
  }

  @Override
  public char[] call() throws Exception
  {
    return args[0].toCharArray();
  }
}
