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

package sample.test;

import java.util.UUID;

import org.jppf.client.JPPFClient;
import org.jppf.client.event.*;

/**
 * Runner for the hello world application.
 * @author Laurent Cohen
 */
public class ClientTest
{
  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    JPPFClient client = null;
    try
    {
      ClientListener listener = new ClientListener()
      {
        @Override
        public void newConnection(final ClientEvent event)
        {
          System.out.println("newConnection: " + event);
        }

        @Override
        public void connectionFailed(final ClientEvent event)
        {
          System.out.println("connectionFailed: " + event);
        }
      };
      //client = new JPPFClient(UUID.randomUUID().toString(), listener);
      client = new JPPFClient(UUID.randomUUID().toString());
      client.addClientListener(listener);
      int sizeInit = client.getAllConnections().size();
      System.out.println("sizeInit = " + sizeInit);
      Thread.sleep(5000L);
      int sizeDone = client.getAllConnections().size();
      System.out.println("sizeDone = " + sizeDone);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
  }
}
