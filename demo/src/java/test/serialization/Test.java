/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package test.serialization;

import java.io.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.serialization.*;
import org.jppf.server.protocol.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class Test
{
  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      test2();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Perform a test.
   * @throws Exception if any error occurs.
   */
  public static void test() throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName("my job id");
    JPPFTask task = new LongTask(1000);
    task.setId("someID");
    task.setResult("result");
    task.setException(new JPPFException("exception"));
    job.addTask(task);
    Test test = new Test();
    byte[] data = test.serialize(job);
    JPPFJob job2 = (JPPFJob) test.deserialize(data);
    print("the end");
  }

  /**
   * Perform a test.
   * @throws Exception if any error occurs.
   */
  public static void test2() throws Exception
  {
    JPPFTaskBundle bundle = new JPPFTaskBundle();
    bundle.setName("server handshake");
    bundle.setUuid("job uuid");
    bundle.setBundleUuid("bundle_uuid");
    bundle.setRequestUuid("0");
    bundle.getUuidPath().add("driver_uuid");
    bundle.setTaskCount(0);
    bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
    Test test = new Test();
    byte[] data = test.serialize(bundle);
    JPPFTaskBundle bundle2 = (JPPFTaskBundle) test.deserialize(data);
    print("the end 2");
  }

  /**
   * Print a string.
   * @param s the string to print.
   */
  private static void print(final String s)
  {
    System.out.println(s);
  }

  /**
   * Serialize an object.
   * @param o the object to serialize.
   * @return the serialized object as a byte array.
   * @throws Exception if any error occurs.
   */
  public byte[] serialize(final Object o) throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    JPPFObjectOutputStream joos = new JPPFObjectOutputStream(baos);
    joos.writeObject(o);
    return baos.toByteArray();
  }

  /**
   * Deserialize an object.
   * @param data the byte array to deserialize from.
   * @return an Object.
   * @throws Exception if any error occurs.
   */
  public Object deserialize(final byte[] data) throws Exception
  {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    JPPFObjectInputStream jois = new JPPFObjectInputStream(bais);
    return jois.readObject();
  }
}
