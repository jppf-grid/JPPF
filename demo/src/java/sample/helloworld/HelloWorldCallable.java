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
package sample.helloworld;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * A simple hello world JPPF task implemented as a <code>Callable</code>.
 * @author Laurent Cohen
 */
public class HelloWorldCallable implements Callable<String>, Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Execute the task.
   * @return a string
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public String call() {
    final String hello = "Hello, World (callable)";
    System.out.println(hello);
    return hello;
  }
}
