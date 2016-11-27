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

package sample.test.deadlock;

/**
 * 
 * @author Laurent Cohen
 */
public class TaskOptions {
  /**
   * Duration of each task in ms.
   */
  public long taskDuration;
  /**
   * Whether the tasks should consume CPU rather than just idling via a call to Thread.sleep() for their assigned duration.
   */
  public boolean useCPU;
  /**
   * Size in bytes of the data associated with each task. If < 0 then a null byte[] is initialized, otherwise a bye[] of the specified size.
   */
  public int dataSize;

}
