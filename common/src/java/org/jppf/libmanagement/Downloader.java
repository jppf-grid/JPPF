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

package org.jppf.libmanagement;

import java.io.*;
import java.util.zip.*;

import org.jppf.location.*;
import org.jppf.utils.FileUtils;
import org.jppf.utils.streams.StreamUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class Downloader {
  /**
   * A location listener used to show the progress of the download.
   */
  private LocationEventListener listener = null;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      Downloader downloader = new Downloader();
      downloader.extractFiles("http://downloads.sourceforge.net/jfreechart/jfreechart-1.0.12.zip", "lib", "jfreechart-1.0.12.jar", "jcommon-1.0.15.jar");
      System.out.println("done");
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Default constructor.
   */
  public Downloader() {
  }

  /**
   * Extract the specified files from the specified archive.
   * @param sourceUrl the URL pointing to the archive to download.
   * @param destPath the folder in which to extract the files.
   * @param names the names of the zip entries to extract.
   * @throws Exception if any IO error occurs.
   */
  public void extractFiles(final String sourceUrl, final String destPath, final String... names) throws Exception {
    File tmp = null;
    try {
      File dir = new File(destPath);
      if (checkFilesPresent(dir, names)) {
        System.out.println("The files are already present in the destination folder");
        return;
      }
      Location source = new URLLocation(sourceUrl);
      tmp = File.createTempFile("jppf_", ".tmp");
      Location dest = new FileLocation(tmp);
      System.out.println("downloading " + source);
      LocationEventListener l = listener;
      if (l == null) l = new LocationEventListener() {
        private int count = 0;

        @Override
        public void dataTransferred(final LocationEvent event) {
          long oneMB = 1024 * 1024;
          long n = event.getTransferredBytes();
          long p = count % oneMB;
          if (n + p >= oneMB) System.out.println(String.valueOf(((n + count) / oneMB)) + " MB downloaded");
          count += n;
        }
      };
      source.addLocationEventListener(l);
      source.copyTo(dest);
      System.out.println("downloaded to " + dest);
      ZipFile zip = new ZipFile(tmp);
      if (!dir.mkdirs()) throw new IOException("Could not create the directories for " + dir);
      for (String name : names) {
        ZipEntry entry = zip.getEntry("jfreechart-1.0.12/lib/" + name);
        InputStream is = zip.getInputStream(entry);
        File f = new File("lib/" + name);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
        StreamUtils.copyStream(is, os);
        System.out.println("extracted " + entry.getName() + " to " + f);
      }
      if (l != null) source.removeLocationEventListener(l);
    } finally {
      if (tmp != null) tmp.delete();
    }
  }

  /**
   * Check that the specified files are present in the specified path.
   * @param folder the folder in which to check for the files.
   * @param names the names of the files to lookup.
   * @return true if the folder and the files exist.
   */
  public boolean checkFilesPresent(final File folder, final String... names) {
    if (!folder.exists() || !folder.isDirectory()) return false;
    File[] files = FileUtils.toFiles(folder, names);
    for (File f : files)
      if (!f.exists()) return false;
    return true;
  }

  /**
   * Get the location listener used to show the progress of the download.
   * @return a <code>LocationEventListener</code> instance.
   */
  public LocationEventListener getListener() {
    return listener;
  }

  /**
   * Set the location listener used to show the progress of the download.
   * @param listener a <code>LocationEventListener</code> instance.
   */
  public void setListener(final LocationEventListener listener) {
    this.listener = listener;
  }
}
