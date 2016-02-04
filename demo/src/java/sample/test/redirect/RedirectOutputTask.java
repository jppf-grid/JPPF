/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package sample.test.redirect;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task simply prints a message.
 * @author Laurent Cohen
 */
public class RedirectOutputTask extends AbstractTask<String[]> {
  @Override
  public void run() {
    String[] output = null;
    try {
      ConsoleOutputRedirector.startRedirect();
      System.out.println("task '" + getId() + "' on standard output");
      System.err.println("task '" + getId() + "' on error output");
    }
    finally {
      output = ConsoleOutputRedirector.endRedirect();
    }
    setResult(output);
  }
}
