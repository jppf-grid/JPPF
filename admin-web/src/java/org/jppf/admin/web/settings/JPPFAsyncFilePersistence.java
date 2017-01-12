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

package org.jppf.admin.web.settings;

import java.io.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * File-based persistence, using an asynchronous write-behind mechanism.
 * @author Laurent Cohen
 */
public class JPPFAsyncFilePersistence extends AbstractFilePersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFAsyncFilePersistence.class);
  /**
   * Used to sequentialize the file wwrites from a single queue.
   */
  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new JPPFThreadFactory(JPPFAsyncFilePersistence.class.getSimpleName()));

  /**
   * 
   */
  JPPFAsyncFilePersistence() {
  }

  @Override
  public void saveString(final String name, final String settings) throws Exception {
    EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        try {
          File file = new File(FileUtils.getJPPFTempDir(), name + ".settings");
          FileUtils.writeTextFile(file, settings);
        } catch (IOException e) {
          log.error("error writing settings to file", e);
        }
      }
    });
  }
}
