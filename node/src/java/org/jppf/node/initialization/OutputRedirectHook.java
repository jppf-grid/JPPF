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

package org.jppf.node.initialization;

import java.io.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This initialization redirects the dirver's or node's stdout and stderr to specified files.
 * @author Laurent Cohen
 */
public class OutputRedirectHook implements InitializationHook {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OutputRedirectHook.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  @Override
  public void initializing(final UnmodifiableTypedProperties config) {
    handleStream(config, true);
    handleStream(config, false);
  }

  /**
   * Handle the specified out redirection from the specified configuration.
   * @param config the un-modified configuration properties of the node at startup time.
   * @param isOut whetehr to handle the output console (System.out) or the error console (System.err).
   */
  private void handleStream(final UnmodifiableTypedProperties config, final boolean isOut) {
    try {
      String propBase = "jppf.redirect." + (isOut ? "out" :  "err");
      File outFile = config.getFile(propBase);
      if (outFile == null) return;
      boolean append = config.getBoolean(propBase + ".append", false);
      OutputStream os = new FileOutputStream(outFile, append);
      if (os != null) {
        System.setOut(new PrintStream(os));
      }
    } catch (Exception e) {
      String message = "error occurred while trying to redirect System." + (isOut ? "out" :  "err") + " : {}";
      if (debugEnabled) log.debug(message, ExceptionUtils.getStackTrace(e));
      else log.warn(message, ExceptionUtils.getMessage(e));
    }
  }
}
