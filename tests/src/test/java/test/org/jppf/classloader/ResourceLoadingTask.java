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
package test.org.jppf.classloader;

import java.net.URL;
import java.util.*;

import org.jppf.node.protocol.AbstractTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class ResourceLoadingTask extends AbstractTask<List<List<URL>>> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * 
   */
  public static final String RES_NAME = "test/org/jppf/classloader/some_resource.txt";
  /**
   * The number of lookups to perform.
   */
  private final int nbLookups;

  /**
   * 
   * @param nbLookups number of lookups to perform.
   */
  public ResourceLoadingTask(final int nbLookups) {
    this.nbLookups = nbLookups;
  }

  @Override
  public void run() {
    try {
      final List<List<URL>> list = new ArrayList<>();
      for (int i=0; i<nbLookups; i++) {
        final Enumeration<URL> urls = getClass().getClassLoader().getResources(RES_NAME);
        if (urls == null) list.add(null);
        else {
          final List<URL> sublist = new ArrayList<>();
          while (urls.hasMoreElements()) sublist.add(urls.nextElement());
          list.add(sublist);
        }
      }
      setResult(list);
    } catch(final Exception e) {
      setThrowable(e);
    }
  }
}
