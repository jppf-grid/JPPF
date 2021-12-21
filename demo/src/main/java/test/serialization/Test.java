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

package test.serialization;

import java.io.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.serialization.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class Test {
  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      final Hello hello = new Hello();
      try {
        oos.writeObject(hello);
      } finally {
        oos.close();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Perform a test.
   * @throws Exception if any error occurs.
   */
  public static void test() throws Exception {
    final JPPFJob job = new JPPFJob();
    job.setName("my job id");
    final Task<String> task = new LongTask(1000);
    task.setId("someID");
    task.setResult("result");
    task.setThrowable(new JPPFException("exception"));
    job.add(task);
    final Test test = new Test();
    final byte[] data = test.serialize(job);
    test.deserialize(data);
    print("the end");
  }

  /**
   * Perform a test.
   * @throws Exception if any error occurs.
   */
  public static void test2() throws Exception {
    final TaskBundle bundle = new JPPFTaskBundle();
    bundle.setName("server handshake");
    bundle.setUuid("job uuid");
    bundle.setUuid("0");
    bundle.getUuidPath().add("driver_uuid");
    bundle.setTaskCount(0);
    bundle.setHandshake(true);
    final Test test = new Test();
    final byte[] data = test.serialize(bundle);
    test.deserialize(data);
    print("the end 2");
  }

  /**
   * Perform a test.
   * @throws Exception if any error occurs.
   */
  public static void test3() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    final Hello hello = new Hello();
    try {
      oos.writeObject(hello);
    } finally {
      oos.close();
    }
  }

  /**
   * Print a string.
   * @param s the string to print.
   */
  private static void print(final String s) {
    System.out.println(s);
  }

  /**
   * Serialize an object.
   * @param o the object to serialize.
   * @return the serialized object as a byte array.
   * @throws Exception if any error occurs.
   */
  public byte[] serialize(final Object o) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); JPPFObjectOutputStream joos = new JPPFObjectOutputStream(baos)) {
      joos.writeObject(o);
      return baos.toByteArray();
    }
  }

  /**
   * Deserialize an object.
   * @param data the byte array to deserialize from.
   * @return an Object.
   * @throws Exception if any error occurs.
   */
  public Object deserialize(final byte[] data) throws Exception {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data); JPPFObjectInputStream jois = new JPPFObjectInputStream(bais)) {
    return jois.readObject();
    }
  }

  /**
   * 
   */
  public static class Hello implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 12345678L;
    /**
     * 
     */
    public String fname = "Lolo";
    /**
     * 
     */
    public String lname = "Coco";
  }
}
