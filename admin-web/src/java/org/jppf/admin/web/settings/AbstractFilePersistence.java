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

package org.jppf.admin.web.settings;

import java.io.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * File-based settings persistence.
 * @author Laurent Cohen
 */
public abstract class AbstractFilePersistence extends AbstractPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractFilePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  @Override
  public String loadString(final String name) throws Exception {
    final File file = new File(FileUtils.getJPPFTempDir(), name + ".settings");
    if (debugEnabled) log.debug("loading settings from file {}", file);
    return file.exists() ? FileUtils.readTextFile(file) : null;
  }
}
