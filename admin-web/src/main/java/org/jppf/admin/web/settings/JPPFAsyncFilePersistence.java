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

package org.jppf.admin.web.settings;

import java.io.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * File-based persistence, using an asynchronous write-behind mechanism for save operations.
 * @author Laurent Cohen
 */
public class JPPFAsyncFilePersistence extends AbstractFilePersistence {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFAsyncFilePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to sequentialize the file wwrites from a single queue.
   */
  private final ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory(JPPFAsyncFilePersistence.class.getSimpleName()));

  /**
   * 
   */
  public JPPFAsyncFilePersistence() {
  }

  @Override
  public void saveString(final String name, final String settings) throws Exception {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        final File file = new File(FileUtils.getJPPFTempDir(), name + ".settings");
        if (settings == null) {
          if (debugEnabled) log.debug("attempting to write null string to '{}'", name);
          if (file.exists() && !file.delete()) log.debug("could not delete file %s", file);
        } else {
          try {
            FileUtils.writeTextFile(file, settings);
          } catch (final Exception e) {
            if (debugEnabled) log.debug("error writing to file '{}' : {}", file, settings);
            else log.error("error writing settings to file", e);
          }
        }
      }
    });
  }

  @Override
  public void close() {
    if (executor != null) executor.shutdownNow();
  }
}
