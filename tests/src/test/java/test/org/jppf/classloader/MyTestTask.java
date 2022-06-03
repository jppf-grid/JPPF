/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.classloader;

import java.util.concurrent.*;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.concurrent.JPPFThreadFactory;

/**
 * 
 * @author Laurent Cohen
 */
public class MyTestTask extends AbstractTask<String> {
  @Override
  public void run() {
    final ExecutorService executor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("Test"));
    try {
      final Callable<String> callable = () -> "hello from " + new MyClass().getName();
      final String msg = executor.submit(callable).get();
      //final String msg = callable.call();
      setResult(msg);
    } catch (final Exception e) {
      setThrowable(e);
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Class loaded during the execution of a callable sublitted to an {@link ExecutorService}.
   */
  public static class MyClass {
    /**
     * @return an arbiitrary string.
     */
    public String getName() {
      return "task";
    }
  }
}
