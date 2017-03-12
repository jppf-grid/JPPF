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

package org.jppf;

/**
 * Performs static initializations that must be done before anything else.
 * @author Laurent Cohen
 * @exclude
 */
public final class JPPFInitializer {
  /**
   * Name of hte package in which custom URL protocols are implemented.
   */
  private static final String PROTOCOL_PACKAGE = "org.jppf.classloader.resource.protocol";
  /**
   * Indicates whether <code>init()</code> has already been called.
   */
  private static boolean initCalled = false;

  /**
   * Prevent any instanciation of this class.
   */
  private JPPFInitializer() {
  }

  /**
   * Perfom the initializations.
   */
  public static void init() {
    if (initCalled) return;
    try {
      initCalled = true;
      String protocolHandlerProperty = "java.protocol.handler.pkgs";
      String s = System.getProperty(protocolHandlerProperty, null);
      if (s == null) System.setProperty(protocolHandlerProperty, PROTOCOL_PACKAGE);
      else if (s.indexOf(PROTOCOL_PACKAGE) < 0) {
        StringBuilder sb = new StringBuilder(s);
        if (sb.charAt(sb.length() - 1) != '|') sb.append('|');
        sb.append(PROTOCOL_PACKAGE);
        s = sb.toString();
        System.setProperty(protocolHandlerProperty, s);
      }
      // warmup System.nanoTime() for JIT
      long start = System.nanoTime();
      for (int i = 0; i < 20_000; i++) {
        long t = System.nanoTime();
      }
      long elapsed = System.nanoTime() - start;
      //System.out.printf("System.nanoTime() warmup in %,d ns%n", elapsed);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
