/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

/**
 * Interface for classes that which to programmatically trigger a heap dump of the current JVM.
 * Currrently, this only works with Oracle's "standard" and JRockit, along with IBM's, JVMs.
 * @author Laurent Cohen
 * @exclude
 */
public interface HeapDumpCollector
{
  /**
   * Call this method from your application whenever you want to dump the heap snapshot into a file.
   * @return a message describing the outcome.
   * @throws Exception if any error occurs
   */
  String dumpHeap() throws Exception;

  /**
   * This factory class generates instances of <code>HeapDumpCollector</code> based on
   * the vendor name found in <code>System.getProperty("java.vm.vendor")</code>.
   * @exclude
   */
  public static class Factory
  {
    /**
     * Create a heap dump collector for the current JVM.
     * @return a <code>HeapDumpCollector</code> instance, or <code>null</code> if none could be created.
     */
    public static HeapDumpCollector newInstance()
    {
      String vendor = System.getProperty("java.vm.vendor", "").toLowerCase();
      if (vendor.indexOf("ibm") >= 0) return new HeapDumpCollectorIBM();
      else if (vendor.indexOf("oracle") >= 0) return new HeapDumpCollectorOracle();
      return null;
    }
  }
}
