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

package org.jppf.example.mbean;

/**
 * Implementation of the AvailableProcessorsMBean interface.
 * @author Laurent Cohen
 */
public class AvailableProcessors implements AvailableProcessorsMBean
{
  /**
   * Get the number of processors available to the JVM.
   * @return the available processors as an integer value.
   * @see org.jppf.example.mbean.AvailableProcessorsMBean#queryAvailableProcessors()
   */
  @Override
  public Integer queryAvailableProcessors()
  {
    // we use the java.lang.Runtime API
    return Runtime.getRuntime().availableProcessors();
  }
}
