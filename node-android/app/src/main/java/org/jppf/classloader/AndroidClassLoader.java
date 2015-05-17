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
package org.jppf.classloader;

import android.content.Context;

import org.jppf.android.AndroidHelper;
import org.jppf.location.FileLocation;
import org.jppf.location.Location;
import org.jppf.node.protocol.ClassPath;
import org.jppf.node.protocol.ClassPathElement;
import org.jppf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import dalvik.system.DexClassLoader;

/**
 * .
 */
public class AndroidClassLoader extends AbstractJPPFClassLoader {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Path to the directory where incoming jar/dex files from a job classpath are stored.
   */
  private static final String DEX_IN_DIR = "dex_in";
  /**
   * Path to the directory where optimized dex files are generated.
   */
  private static final String DEX_OUT_DIR = "dex_out";

  /**
   * Initialize this class loader.
   * @param parent  the parent class loader.
   * @param classpath location of jars with dex files.
   */
  public AndroidClassLoader(final ClassLoader parent, ClassPath classpath) {
    super(null, createDexClassLoader(parent, classpath), null);
  }

  /**
   * Create a {@link DexClassLoader} based on the specified classpath.
   * @param parent  the parent class loader.
   * @param classpath contains the locations of jar files with dex files. In principle these should only be memory locations.
   * @return a new {@link DexClassLoader} instance.
   */
  private static DexClassLoader createDexClassLoader(final ClassLoader parent, ClassPath classpath) {
    StringBuilder pathBuilder = new StringBuilder();
    int count = 0;
    File inDir = AndroidHelper.getActivity().getDir(DEX_IN_DIR, Context.MODE_PRIVATE);
    FileUtils.deletePath(inDir, true);
    File outDir = AndroidHelper.getActivity().getDir(DEX_OUT_DIR, Context.MODE_PRIVATE);
    FileUtils.deletePath(outDir, true);
    if (classpath != null) {
      for (ClassPathElement element: classpath) {
        File file = createDexFile(element, inDir);
        if (count > 0) pathBuilder.append(File.pathSeparator);
        pathBuilder.append(file.getAbsolutePath());
        count++;
      }
    }
    DexClassLoader loader = new DexClassLoader(pathBuilder.toString(), outDir.getAbsolutePath(), null, parent);
    return loader;
  }

  /**
   *
   * @param element location of jars with dex files.
   * @return a new {@link DexClassLoader} instance.
   */
  private static File createDexFile(final ClassPathElement element, final File inDir) {
    Location<?> inLoc = element.getRemoteLocation();
    File file = new File(inDir, element.getName());
    try {
      Location<?> outLoc = new FileLocation(file);
      inLoc.copyTo(outLoc);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return file;
  }

  @Override
  void reset() {
  }
}
