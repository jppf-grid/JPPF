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

package org.jppf.client.taskwrapper;

import org.jppf.node.protocol.DataProvider;

/**
 * This interface must be implemented by tasks that are not implementations of {@link org.jppf.node.protocol.Task Task<T>}
 * when they need access to the job's {@link DataProvider}.
 * <p>Implementations should ensure the DataProvider is not kept as a persistent (on-transient) attribute of the
 * implementing class, otherwise it will be serialized along with the task after execution, causing additional CPU usage
 * and network traffic. 
 * @author Laurent Cohen
 */
public interface DataProviderHolder
{
  /**
   * Set the data provider for the task.
   * @param dataProvider the data provider set onto the job to which the tasks belongs.
   */
  void setDataProvider(DataProvider dataProvider);
}
