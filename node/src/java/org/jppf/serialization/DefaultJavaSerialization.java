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

package org.jppf.serialization;

import java.io.*;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This implementation uses the default Java serialization.
 */
public class DefaultJavaSerialization implements JPPFSerialization {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DefaultJavaSerialization.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    new ObjectOutputStream(os).writeObject(o);
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    ObjectInputStream ois = new ObjectInputStream(is) {
      @Override
      protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
          if (traceEnabled) log.trace("{} thread context class loader is null, current call stack: {}", getClass().getSimpleName(), ExceptionUtils.getCallStack());
          return super.resolveClass(desc);
        }
        return Class.forName(desc.getName(), false, cl);
      }
    };
    return ois.readObject();
  }
}
