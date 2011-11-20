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
package sample.helloworld;

import java.io.Serializable;

/**
 * A simple hello world JPPF task implemented as a POJO with a constructor.
 * @author Laurent Cohen
 */
public class HelloWorldPojoConstructor implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The string resulting from the task execution.
   */
  private String hello = null;

  /**
   * Execute the task.
   * @param message a message to print.
   * @param number an example primitive argument.
   */
  public HelloWorldPojoConstructor(final String message, final int number)
  {
    this.hello = "Hello, World (pojo constructor, " + message + ", " + number + ')';
    System.out.println(this.hello);
  }

  /**
   * Get the string resulting from the task execution.
   * @return a string.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return hello;
  }
}
