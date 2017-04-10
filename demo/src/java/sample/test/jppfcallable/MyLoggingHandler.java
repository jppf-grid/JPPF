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

package sample.test.jppfcallable;

import java.io.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.*;

import org.jppf.logging.jmx.JmxLogger;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class MyLoggingHandler extends ThreadSynchronization implements NotificationListener, Runnable {
  /**
   * The path of the file to which the logging statement arte printed.
   */
  private String outputFileNazme = "driver-log.txt";
  /**
   * The queue where the notifications are stored before being printed to file.
   */
  private Queue<String> queue = new ConcurrentLinkedQueue<>();
  /**
   * The writer printing the notifications to a file.
   */
  private Writer writer = null;

  /**
   * Default constructor.
   * @throws Exception if any error occurs.
   */
  public MyLoggingHandler() throws Exception {
    writer = new BufferedWriter(new FileWriter(outputFileNazme));
    new Thread(this, "LoggingHandler").start();
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    String message = notification.getMessage();
    queue.offer(message);
    wakeUp();
  }

  @Override
  public void run() {
    String msg = null;
    try {
      while (!isStopped()) {
        int count = 0;
        while ((msg = queue.poll()) != null) {
          writer.write(msg);
          count++;
        }
        if (count > 0) writer.flush();
        if (isStopped()) break;
        goToSleep(1L, 0);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      StreamUtils.closeSilent(writer);
    }
  }

  /**
   * Register this logging handler to receive notifications from the specified MBean.
   * @param jmxLogger a proxy to the MBean which emits the notifications.
   */
  public void register(final JmxLogger jmxLogger) {
    queue.offer(StringUtils.padRight("", '-', 80) + '\n');
    jmxLogger.addNotificationListener(this, null, null);
  }

  /**
   * Unregister this logging handler from the specified MBean.
   * @param jmxLogger a proxy to the MBean which emits the notifications.
   */
  public void unregister(final JmxLogger jmxLogger) {
    try {
      jmxLogger.removeNotificationListener(this);
    } catch (@SuppressWarnings("unused") ListenerNotFoundException ignore) {
    }
  }
}
