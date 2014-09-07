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

package org.jppf.process;

import java.util.*;

/**
 * This class holds the constant for actions handled by a {@link LauncherListenerProtocolHandler}.
 * @author Laurent Cohen
 * @since 5.0
 */
public final class ProcessCommands {
  /**
   * Request a restart with {@code interruptIfRunning = true}.
   */
  public final static int RESTART_INTERRUPT = 1;
  /**
   * Request a restart with {@code interruptIfRunning = false}.
   */
  public final static int RESTART_NO_INTERRUPT = 2;
  /**
   * Request a shutdown with {@code interruptIfRunning = true}.
   */
  public final static int SHUTDOWN_INTERRUPT = 3;
  /**
   * Request a shutdown with {@code interruptIfRunning = false}.
   */
  public final static int SHUTDOWN_NO_INTERRUPT = 4;
  /**
   *
   */
  private final static Map<Integer, String> commandMap = new HashMap<>();
  static {
    registerCommand(RESTART_INTERRUPT, "RESTART_INTERRUPT");
    registerCommand(RESTART_NO_INTERRUPT, "RESTART_NO_INTERRUPT");
    registerCommand(SHUTDOWN_INTERRUPT, "SHUTDOWN_INTERRUPT");
    registerCommand(SHUTDOWN_NO_INTERRUPT, "SHUTDOWN_NO_INTERRUPT");
  }

  /**
   * Instantiation is not permitted.
   */
  private ProcessCommands() {
  }

  /**
   *
   * @param command the command code.
   * @param name the nassociated command name.
   * @return the command code.
   */
  private static int registerCommand(final int command, final String name) {
    commandMap.put(command, name);
    return command;
  }

  /**
   * Get the name associated with a command code.
   * @param command the command code to lookup.
   * @return a related name.
   */
  public static String getCommandName(final int command) {
    String s = commandMap.get(command);
    return s == null ? Integer.valueOf(command).toString() : s;
  }
}
