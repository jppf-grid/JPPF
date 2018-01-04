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

package test.org.jppf.test.setup;

import java.io.*;
import java.util.zip.*;

/**
 * Utility methods for zipping/unzipping files.
 * @author Laurent Cohen
 */
public class ZipUtils {
  /**
   * Zip the specified files into the specified zip.
   * @param zipPath the path of the zip file to create.
   * @param paths the paths of the files to add to the zip.
   */
  static void zipFile(final String zipPath, final String...paths) {
    final File zipFile = new File(zipPath);
    try (final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
      final byte[] buffer = new byte[2048];
      for (final String path: paths) {
        final File inputFile = new File(path);
        try (final InputStream is = new BufferedInputStream(new FileInputStream(inputFile))) {
          final ZipEntry entry = new ZipEntry(inputFile.getName());
          zos.putNextEntry(entry);
          int n;
          while ((n = is.read(buffer)) > 0) {
            zos.write(buffer, 0, n);
          }
        }
      }
      zos.finish();
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
}
