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

package sample.test;

import java.io.Serializable;

import org.jppf.node.protocol.JPPFRunnable;

/**
 * Class used to test the annotated task wrapper.
 * @author Laurent Cohen
 */
public class TestAnnotatedTask implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The method to run.
   * @param intArg an int argument.
   * @param stringArg a string argument.
   * @return the result as a string.
   */
  @JPPFRunnable
  public String someMethod(final int intArg, final String stringArg)
  {
    String s = "int arg = " + intArg + ", string arg = \"" + stringArg + '\"';
    System.out.println(s);
    return s;
  }
}
