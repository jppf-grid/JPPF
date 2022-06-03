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

package test.org.jppf.server.protocol;

import java.io.*;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.streams.StreamUtils;

/**
 * A task that creates a file.
 */
public class FileTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /** */
  private final String filePath;
  /** */
  private final boolean appendNodeSuffix;

  /**
   * Initialize this task with the specified file path.
   * @param filePath the path of the file to create.
   * @param appendNodeSuffix <code>true</code> to append the node name to the file's name, <code>false</code> otherwise.
   */
  public FileTask(final String filePath, final boolean appendNodeSuffix) {
    this.filePath = filePath;
    this.appendNodeSuffix = appendNodeSuffix;
  }

  @Override
  public void run() {
    try {
      String name = filePath;
      if (appendNodeSuffix) name = name + getNode().getUuid();
      name = name + ".tmp";
      System.out.printf("creating file '%s'", name);
      final File f = new File(name);
      Thread.sleep(2000L);
      final Writer writer = new FileWriter(f);
      StreamUtils.closeSilent(writer);
    } catch (final Exception e) {
      setThrowable(e);
      e.printStackTrace();
    }
  }
}