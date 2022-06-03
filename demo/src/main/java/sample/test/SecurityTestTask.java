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
package sample.test;

import java.io.*;
import java.net.Socket;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * This task is intended for testing the framework only.
 * @author Laurent Cohen
 */
public class SecurityTestTask extends JPPFTestTask {
  /**
   * Initialize this task.
   */
  public SecurityTestTask() {
  }

  /**
   * Try exiting the JVM through a <code>System.exit(int)</code> call.
   * @throws SecurityException if the security manager prevents from exiting the JVM.
   */
  public void testExitVM() throws SecurityException {
    System.exit(0);
  }

  /**
   * Try connecting to a non authorized host through a TCP/IP socket.
   * @throws SecurityException if the security manager prevents from connecting to the host.
   */
  public void testConnectForbiddenHost() throws SecurityException {
    try {
      final Socket s = new Socket("www.apache.org", 8000);
      s.close();
    } catch (@SuppressWarnings("unused") final IOException e) {
    }
  }

  /**
   * Try connecting to a non authorized port on the JPPF server.
   * @throws SecurityException if the security manager prevents from connecting on the specified port.
   */
  public void testConnectForbiddenPort() throws SecurityException {
    try {
      final String host = JPPFConfiguration.get(JPPFProperties.SERVER_HOST);
      final Socket s = new Socket(host, 1001);
      s.close();
    } catch (@SuppressWarnings("unused") final IOException e) {
    }
  }

  /**
   * Try writing a dummy file.
   * @throws SecurityException if the security manager prevents from writing the file.
   */
  public void testWriteFile() throws SecurityException {
    try {
      final FileWriter writer = new FileWriter("foo.bar");
      writer.write("Hello");
      writer.close();
    } catch (@SuppressWarnings("unused") final IOException e) {
    }
  }

  /**
   * Try reading a non-authorized file.
   * @throws SecurityException if the security manager prevents from writing the file.
   */
  public void testReadFile() throws SecurityException {
    try {
      final File file = new File("/");
      final File[] dirList = file.listFiles();
      for (File f : dirList) {
        if (!f.isDirectory()) {
          final FileInputStream fis = new FileInputStream(f);
          fis.read();
          fis.close();
          break;
        }
      }
    } catch (@SuppressWarnings("unused") final IOException e) {
    }
  }
}
