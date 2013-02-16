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

package org.jppf.server.protocol;

import java.io.*;


/**
 * This class optimizes the serialization behavior of JPPFTask, by only handling
 * the <code>getResult()</code> and <code>getException()</code> fields of the task once it has been executed.
 * This reduces the serialization and network transport overhead, especially for tasks that use large amounts of input data,
 * or generate large amount of instance data during their execution.
 * <p>This class should be used as in the following example:
 * <pre>
 * public class MyTask extends JPPFOptimizedTask {
 * 
 *   private MyClass myField = ...;
 * 
 *   public void run() {
 *       // ...
 *   }
 * 
 *   private void writeObject(ObjectOutputStream out) throws IOException {
 *     serialize(out);
 *   }
 * 
 *   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
 *     deserialize(in);
 *   }
 * }
 * </pre>
 * @author Laurent Cohen
 * @exclude
 */
public abstract class JPPFOptimizedTask extends JPPFTask
{
  /**
   * Counts the number of times this task was serialized.
   */
  private byte serializationCount = 0;
  /**
   * Counts the number of times this task was deserialized.
   */
  private byte deserializationCount = 0;

  /**
   * Serialize this task.
   * The first time it is serialized, all the non-static and non-transient fields will be serialized.
   * Upon subsequent serializations, only <code>getResult()</code> and <code>getException()</code> will be serialized.
   * <p>This method is to be called only from the <code>writeObject(ObjectOutputStream)</code> method of a subclass.
   * @param out the object output stream to write to.
   * @throws IOException if any error occurs.
   * @see java.io.Serializable
   */
  protected void serialize(final ObjectOutputStream out) throws IOException
  {
    int count = serializationCount;
    out.write(++serializationCount);
    out.write(deserializationCount);
    // already been serialized, presumably by the client?
    if (count > 0)
    {
      out.writeInt(getPosition());
      out.writeObject(getId());
      out.writeObject(getResult());
      out.writeObject(getException());
    }
    else
    {
      out.defaultWriteObject();
    }
  }

  /**
   * Deserialize this task.
   * The first time it is deserialized, all the non-static and non-transient fields will be deserialized.
   * Upon subsequent deserializations, only <code>getResult()</code> and <code>getException()</code> will be deserialized.
   * <p> This method is to be called only from the <code>readObject(ObjectInputStream)</code> method of a subclass.
   * @param in the stream to read from.
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if a class could not be found.
   */
  protected void deserialize(final ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    serializationCount = (byte) in.read();
    deserializationCount = (byte) in.read();
    // already been deserialized, presumably by the node?
    if (deserializationCount > 0)
    {
      setPosition(in.readInt());
      setId((String) in.readObject());
      setResult(in.readObject());
      setException((Exception) in.readObject());
    }
    else
    {
      in.defaultReadObject();
    }
    deserializationCount++;
  }
}
