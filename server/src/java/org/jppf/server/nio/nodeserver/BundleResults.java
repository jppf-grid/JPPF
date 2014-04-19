/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import java.util.List;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.utils.Pair;

/**
 * Convenience class to simplify and improve readability of the code.
 * @author Laurent Cohen
 */
public class BundleResults extends Pair<TaskBundle, List<DataLocation>>
{

  /**
   * Initialize with the specified task bundle and task data.
   * @param bundle the header received from the node.
   * @param data a list of {@link DataLocation} instances.
   */
  public BundleResults(final TaskBundle bundle, final List<DataLocation> data)
  {
    super(bundle, data);
  }

  /**
   * Get the task bundle.
   * @return a {@link TaskBundle} instance.
   */
  public TaskBundle bundle()
  {
    return first();
  }

  /**
   * Return the task data.
   * @return a list of {@link DataLocation} instances.
   */
  public List<DataLocation> data()
  {
    return second();
  }
}
