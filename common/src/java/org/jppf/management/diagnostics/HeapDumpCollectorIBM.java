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

package org.jppf.management.diagnostics;

import java.lang.reflect.Method;

import org.jppf.JPPFException;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Generate a heap dump for an IBM JVM.
 * @author Laurent Cohen
 * @exclude
 */
public class HeapDumpCollectorIBM implements HeapDumpCollector
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(HeapDumpCollectorIBM.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The method to invoke to get a heap dump.
   */
  private static Method heapdumpMethod = getDumpMethod();

  @Override
  public String dumpHeap() throws Exception
  {
    try
    {
      if (heapdumpMethod == null) throw new JPPFException("Dump class is not avaialable - no heap dump taken");
      heapdumpMethod.invoke(null, (Object[]) null);
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      throw e;
    }
    return "heap dump saved";
  }

  /**
   * Get the method to invoke to get a heap dump.
   * @return a {@link Method} instance, or null if the method could not be found.
   */
  private static Method getDumpMethod()
  {
    try
    {
      Class<?> clazz = Class.forName("com.ibm.jvm.Dump");
      return clazz.getDeclaredMethod("HeapDump", (Class<?>[]) null);
    }
    catch (Exception e)
    {
      return null;
    }
  }
}
