/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.io.*;
import java.util.UUID;

import javax.swing.text.html.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.event.*;

/**
 * Runner for the hello world application.
 * @author Laurent Cohen
 */
public class ClientTest {
  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args) {
    JPPFClient client = null;
    try {
      final ConnectionPoolListener listener = new ConnectionPoolListenerAdapter() {
        @Override
        public void connectionAdded(final ConnectionPoolEvent event) {
          System.out.println("newConnection: " + event);
        }

        @Override
        public void connectionRemoved(final ConnectionPoolEvent event) {
          System.out.println("connectionFailed: " + event);
        }
      };
      client = new JPPFClient(UUID.randomUUID().toString());
      client.addConnectionPoolListener(listener);
      final int sizeInit = client.getAllConnectionsCount();
      System.out.println("sizeInit = " + sizeInit);
      Thread.sleep(5000L);
      final int sizeDone = client.getAllConnectionsCount();
      System.out.println("sizeDone = " + sizeDone);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * 
   * @throws Exception .
   */
  public static void testHTML() throws Exception {
    final HTMLDocument doc = new HTMLDocument();
    try (final Reader reader = new FileReader("C:/temp/MyFile.html")) {
      final HTMLEditorKit kit = new HTMLEditorKit();
      kit.read(reader, doc, 0);
    }
    //doc.
  }
}
