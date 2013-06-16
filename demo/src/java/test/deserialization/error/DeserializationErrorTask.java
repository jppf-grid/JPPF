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
package test.deserialization.error;

import java.io.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class DeserializationErrorTask extends JPPFTask
{
  /**
   * 
   */
  private int idx = 0;
  /**
   * 
   */
  private Object myObject = null;

  /**
   * Perform initializations.
   * @param idx the idx.
   */
  public DeserializationErrorTask(final int idx)
  {
    this.idx = idx;
    myObject = new MyObject();
    setResult("*** this task was not executed ***");
  }

  @Override
  public void run()
  {
    try
    {
      Thread.sleep(1);
      setResult("the execution was performed successfully");
    }
    catch (Exception e)
    {
      setException(e);
    }
  }

  /**
   * Write this object to the specified stream.
   * @param out the stream to write to.
   * @throws IOException if any error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException
  {
    out.writeInt(idx);
    out.writeObject(myObject);
  }

  /**
   * Read this object from the specified stream.
   * @param in the stream to read from.
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if a class cannot be loaded.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    try
    {
      Thread.sleep(4000L);
    }
    catch (Exception e)
    {
    }
    idx = in.readInt();
    myObject = in.readObject();
  }
}
