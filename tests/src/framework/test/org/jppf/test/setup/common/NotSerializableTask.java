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

package test.org.jppf.test.setup.common;

import org.jppf.server.protocol.JPPFTask;

/**
 * A task which holds a non-serializable object.
 */
public class NotSerializableTask extends JPPFTask
{
  /**
   * A non-serializable object.
   */
  private NotSerializableObject nso = null;
  /**
   *  <code>true</code> if the non-serializable object should be created in the constructor, <code>false</code> if it should be created in the client.
   */
  private final boolean instantiateInClient;

  /**
   * Initialize with the specified flag.
   * @param instantiateInClient <code>true</code> if the non-serializable object should be created in the constructor (client side),
   * <code>false</code> if it should be created in the <code>run()</code> method (node side).
   */
  public NotSerializableTask(final boolean instantiateInClient)
  {
    this.instantiateInClient = instantiateInClient;
    if (instantiateInClient) nso = new NotSerializableObject();
  }

  @Override
  public void run()
  {
    if (!instantiateInClient) nso = new NotSerializableObject();
  }
}