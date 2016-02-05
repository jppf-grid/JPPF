/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.io;

import java.io.File;

/**
 * Instances of this class hold a temporary {@link File} and ensure the file
 * is deleted when they are garbage collected.
 * @author Laurent Cohen
 */
public class TemporaryFileHolder {
  /**
   * The file handled by this file holder.
   */
  private File file = null;

  /**
   * Initialize this file holder with the specified file.
   * @param file the temporary file to handle.
   */
  public TemporaryFileHolder(final File file) {
    this.file = file;
  }

  /**
   * This method deletes the underlying file.
   * @throws Throwable if an error occurs.
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      if ((file != null) && file.exists()) file.delete();
    } finally {
      super.finalize();
    }
  }

  /**
   * Get the underlying file.
   * @return a {@link File} instance.
   */
  public File getFile() {
    return file;
  }
}
