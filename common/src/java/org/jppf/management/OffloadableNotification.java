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

package org.jppf.management;

import java.io.*;

import javax.management.*;

import org.jppf.io.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * A notification that can offload its user data to file to avoid OOMEs.
 * @author Laurent Cohen
 * @exclude
 */
public class OffloadableNotification extends Notification {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(OffloadableNotification.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * 
   */
  private static final long THRESHOLD = parseThreshold();
  /**
   * Points to a file where user data may be serialized.
   */
  private transient DataLocation dataLocation;
  /**
   * Whether the user data is offloaded to file.
   */
  private boolean userDataOffloaded = false;

  /**
   * Initialize this notification with the specified parameters.
   * @param type .
   * @param source the emitter MBean's object name.
   * @param sequenceNumber the notification sequence number.
   * @param timestamp .
   */
  public OffloadableNotification(final String type, final ObjectName source, final long sequenceNumber, final long timestamp) {
    super(type, source, sequenceNumber, timestamp);
  }

  @Override
  public Object getUserData() {
    Object o = null;
    if (userDataOffloaded) {
      if (dataLocation != null) {
        if (debugEnabled) log.debug("getting offloaded user data: dataLocation={}", dataLocation);
        try {
          o = IOHelper.unwrappedData(dataLocation);
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
    else o = super.getUserData();
    return o;
  }

  @Override
  public void setUserData(final Object userData) {
    try {
      final long used = SystemUtils.getUsedMemory();
      if (debugEnabled) log.debug(String.format("used memory=%,d, threshold=%,d", used, THRESHOLD));
      if ((userData != null) && (used >= THRESHOLD)) {
        super.setUserData(null);
        userDataOffloaded = true;
        try {
          dataLocation = IOHelper.serializeDataToFile(userData, IOHelper.getDefaultserializer());
          if (debugEnabled) log.debug(String.format("offloading user data: used memory=%,d, threshold=%,d, dataLocation=%s", used, THRESHOLD, dataLocation));
        } catch (final Exception e) {
          dataLocation = null;
          log.error(e.getMessage(), e);
        }
      } else {
        userDataOffloaded = false;
        super.setUserData(userData);
      }
    } catch (final Error e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Save the state of the notification to a stream (i.e. serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    if (userDataOffloaded) {
      final OutputDestination dest = new StreamOutputDestination(out);
      try {
        IOHelper.writeData(dataLocation, dest);
      } catch (final IOException e) {
        throw e;
      } catch (final Exception e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * Reconstitute the notification instance from an object stream (i.e. deserialize it).
   * @param in the input stream from which to read the job. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (userDataOffloaded) {
      try {
        dataLocation = IOHelper.readData(new StreamInputSource(in));
      } catch (IOException|ClassNotFoundException e) {
        throw e;
      } catch (final Exception e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * Parse a memory size value in format [size][unit] where:
   * <ul>
   * <li>size is a positive {@code long} value</li>
   * <li>unit is one of 'g', 'm', 'k', 'b' or uppercase equivalents 'G', 'M', 'K', 'B'. If the unit string is anything else then it defaults to 'b'</li>
   * </ul>
   * Examples: 2g, 1536M, 123456k, 123456789b
   * @return parse the used memory threshold that triggers user data offloading, from the configuration.
   */
  private static long parseThreshold() {
    final String s = JPPFConfiguration.get(JPPFProperties.NOTIFICATION_OFFLOAD_MEMORY_THRESHOLD);
    char unit = 0;
    int i;
    for (i=0; i<s.length(); i++) {
      final char c = s.charAt(i);
      if (!Character.isDigit(c)) {
        unit = Character.toLowerCase(c);
        break;
      }
    }
    long threshold = 0;
    try {
      threshold = Long.valueOf(s.substring(0, i));
    } catch (@SuppressWarnings("unused") final Exception e) {
      threshold = (long) (0.8d * Runtime.getRuntime().maxMemory());
    }
    switch(unit) {
      case 'g':
        threshold *= 1024L * 1024L * 1024L;
        break;
      case 'm':
        threshold *= 1024L * 1024L;
        break;
      case 'k':
        threshold *= 1024L;
        break;
    }
    return threshold;
  }
}
