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
package sample.test;


/**
 * JPPF task used to test how exceptions are handled within the nodes.
 * @author Laurent Cohen
 */
public class ExceptionTestTask extends JPPFTestTask
{
  /**
   * Default constructor - the method that throws an NPE will be invoked.
   */
  public ExceptionTestTask()
  {
  }

  /**
   * This method throws a <code>NullPointerException</code>.
   */
  protected void testThrowNPE()
  {
    throw new NullPointerException();
  }

  /**
   * This method throws an <code>ArrayIndexOutOfBoundsException</code>.
   */
  protected void testThrowArrayIndexOutOfBoundsException()
  {
    throw new ArrayIndexOutOfBoundsException();
  }

  /**
   * This method throws a <code>SecurityException</code>.
   */
  protected void testThrowSecurityException()
  {
    System.getProperty("throw.security.exception");
  }
}
