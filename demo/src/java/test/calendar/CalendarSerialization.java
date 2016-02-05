/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.calendar;

import java.io.*;
import java.util.Calendar;

/**
 * Test of multithreaded serialization/deserialization of Calendar.
 * @author Laurent Cohen
 */
public class CalendarSerialization
{
  /**
   * 
   * @param args not used
   */
  public static void main(final String[] args)
  {
    try
    {
      new CalendarSerialization().perform();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  public void perform() throws Exception
  {
    int nbThreads = 8;
    Calendar cal = Calendar.getInstance();
    SerializationThread[] threads = new SerializationThread[nbThreads];
    for (int i=0; i<nbThreads; i++) threads[i] = new SerializationThread(cal);
    for (int i=0; i<nbThreads; i++) threads[i].start();
    for (int i=0; i<nbThreads; i++) threads[i].join();
    DeserializationThread[] threads2 = new DeserializationThread[nbThreads];
    for (int i=0; i<nbThreads; i++) threads2[i] = new DeserializationThread(threads[i].data);
    for (int i=0; i<nbThreads; i++) threads2[i].start();
    for (int i=0; i<nbThreads; i++) threads2[i].join();
  }

  /**
   * 
   * @author Laurent Cohen
   */
  public class SerializationThread extends Thread
  {
    /**
     * 
     */
    private Calendar cal;
    /**
     * 
     */
    public byte[] data;

    /**
     * 
     * @param cal .
     */
    public SerializationThread(final Calendar cal)
    {
      this.cal = cal;
    }

    @Override
    public void run()
    {
      try
      {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cal);
        oos.flush();
        oos.close();
        data = baos.toByteArray();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * 
   */
  public class DeserializationThread extends Thread
  {
    /**
     * 
     */
    public Calendar cal;
    /**
     * 
     */
    public byte[] data;

    /**
     * 
     * @param data .
     */
    public DeserializationThread(final byte[] data)
    {
      this.data = data;
    }

    @Override
    public void run()
    {
      try
      {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        cal = (Calendar) ois.readObject();
        ois.close();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
