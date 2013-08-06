/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.Closeable;
import java.net.URL;

/**
 * 
 * @author Laurent Cohen
 */
public interface PathHandler extends Closeable
{
  /**
   * Find the resource with the specified name in this path.
   * @param name the name of the resource to find.
   * @return a URL pointing to the found resource, or null if no resource with this name could be found.
   */
  URL findResource(String name);
}
