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

package test.org.jppf.test.runner;

import java.io.Serializable;

/**
 * Holds an exception that occurs outside of the JUnit runner scope.
 * @author Laurent Cohen
 */
public class ExceptionHolder implements Serializable
{
  /**
   * The exception that was raised.
   */
  private final Throwable throwable;
  /**
   * The name of the test class for which the exception was raised.
   */
  private final String className;

  /**
   * Initialize this exception holder with the specified <code>Throwable</code> and test class name.
   * @param className the name of the test class for which the exception was raised.
   * @param throwable the exception that was raised.
   */
  public ExceptionHolder(final String className, final Throwable throwable)
  {
    this.className = className;
    this.throwable = throwable;
  }

  /**
   * Get the exception that was raised.
   * @return a <code>Throwable</code> instance.
   */
  public Throwable getThrowable()
  {
    return throwable;
  }

  /**
   * Get the name of the test class for which the exception was raised.
   * @return the class name as a <code>String</code>.
   */
  public String getClassName()
  {
    return className;
  }

  @Override
  public String toString()
  {
    return "ExceptionHolder [throwable=" + throwable + ", className=" + className + "]";
  }
}
