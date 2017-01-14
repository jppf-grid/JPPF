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

import java.io.File;

import org.jppf.utils.FileUtils;

/**
 * File-based persistence, using a synchronous write-through mechanism.
 * @author Laurent Cohen
 */
public class JPPFFilePersistence extends AbstractFilePersistence {
  /**
   * 
   */
  JPPFFilePersistence() {
  }

  @Override
  public void saveString(final String name, final String settings) throws Exception {
    File file = new File(FileUtils.getJPPFTempDir(), name + ".settings");
    FileUtils.writeTextFile(file, settings);
  }
}