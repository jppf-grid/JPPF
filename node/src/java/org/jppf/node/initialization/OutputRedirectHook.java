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

package org.jppf.node.initialization;

import java.io.*;
import java.util.Date;

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * This initialization redirects the dirver's or node's stdout and stderr to specified files.
 * @author Laurent Cohen
 * @since 4.1
 */
public class OutputRedirectHook implements InitializationHook {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OutputRedirectHook.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

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
      JPPFProperty<File> pathProp = isOut ? JPPFProperties.REDIRECT_OUT : JPPFProperties.REDIRECT_ERR;
      File outFile = config.get(pathProp);
      if (outFile == null) return;
      if (debugEnabled) log.debug("redirecting System.{} to file {}", (isOut ? "out" : "err"), outFile);
      JPPFProperty<Boolean> appendProp = isOut ? JPPFProperties.REDIRECT_OUT_APPEND : JPPFProperties.REDIRECT_ERR_APPEND;
      boolean append = config.get(appendProp);
      OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile, append));
      PrintStream pos = new PrintStream(os, true);
      pos.println("********** " + new Date() + " **********");
      if (isOut) System.setOut(pos);
      else System.setErr(pos);
    } catch (Exception e) {
      String message = "error occurred while trying to redirect System." + (isOut ? "out" :  "err") + " : {}";
      if (debugEnabled) log.debug(message, ExceptionUtils.getStackTrace(e));
      else log.warn(message, ExceptionUtils.getMessage(e));
    }
  }
}
