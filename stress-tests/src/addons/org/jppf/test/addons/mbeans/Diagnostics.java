/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.test.addons.mbeans;

import java.lang.management.*;

/**
 * 
 * @author Laurent Cohen
 */
public class Diagnostics implements DiagnosticsMBean
{
  /**
   * 
   */
  public Diagnostics()
  {
    System.out.println("initializing " + getClass().getSimpleName());
    /*
    System.out.println(getDiagnosticsInfo());
     */
  }

  @Override
  public DiagnosticsInfo getDiagnosticsInfo()
  {
    return new DiagnosticsInfo();
  }

  @Override
  public void gc()
  {
    System.gc();
  }

  @Override
  public String[] threadNames()
  {
    ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
    long[] ids = threadsBean.getAllThreadIds();
    ThreadInfo[] infos = threadsBean.getThreadInfo(ids, 0);
    String[] result = new String[infos.length];
    for (int i=0; i<infos.length; i++) result[i] = infos[i].getThreadName();
    return result;
  }
}
