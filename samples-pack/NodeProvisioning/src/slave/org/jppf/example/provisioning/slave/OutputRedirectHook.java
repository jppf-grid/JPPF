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

package org.jppf.example.provisioning.slave;

import java.io.PrintStream;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.UnmodifiableTypedProperties;

/**
 * This initialization redirects the save nodes' stdout and stderr to specific files.
 * @author Laurent Cohen
 */
public class OutputRedirectHook implements InitializationHook {
  @Override
  public void initializing(final UnmodifiableTypedProperties initialConfiguration) {
    try {
      PrintStream out = new PrintStream("stdout.log");
      PrintStream err = new PrintStream("stderr.log");
      System.setOut(out);
      System.setErr(err);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
