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

package org.jppf.jca.work;

import org.jppf.comm.socket.*;
import org.slf4j.*;

/**
 * Socket initializer for the JPPF resource adapter.
 * This implementation does not rely on its own threads or timers to not interfere with the application server.
 * @author Laurent Cohen
 */
public class JcaSocketInitializer extends AbstractSocketInitializer
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JcaSocketInitializer.class);
  /**
   * Maximum number of connection attempts.
   */
  private int maxAttempts = 1;

  /**
   * 
   * @see org.jppf.comm.socket.SocketInitializer#close()
   */
  @Override
  public void close()
  {
    closed = true;
  }

  /**
   * Initialize the underlying socket.
   * @param socketWrapper wrapper around the socket to initialize.
   * @see org.jppf.comm.socket.SocketInitializer#initializeSocket(org.jppf.comm.socket.SocketWrapper)
   */
  @Override
  public void initializeSocket(final SocketWrapper socketWrapper)
  {
    attemptCount = 0;
    successfull = false;
    while ((attemptCount < maxAttempts) && !successfull)
    {
      try
      {
        if (socketWrapper.isOpened()) socketWrapper.close();
      }
      catch(Exception ignored)
      {
      }

      try
      {
        socketWrapper.open();
        successfull = true;
      }
      catch (Exception e)
      {
        attemptCount++;
        if (attemptCount < maxAttempts)
        {
          try
          {
            Thread.sleep(10 + rand.nextInt(100));
          }
          catch(InterruptedException e2)
          {
            close();
            log.error(e.getMessage());
            break;
          }
        }
      }
    }
  }
}
