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

package org.jppf.dotnetbridge.nodehook;

import java.io.File;

import net.sf.jni4net.*;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.*;

/**
 * This node initialization hook performs the required jni4net bridge initalizations.
 * @author Laurent Cohen
 */
public class DotnetBridgeHook implements InitializationHook {
  @Override
  public void initializing(final UnmodifiableTypedProperties config) {
    if (!SystemUtils.isWindows()) return;
    try {
      //Bridge.setDebug(true);
      //Bridge.setVerbose(true);
      Bridge.init();
      BridgeSetup setup = Bridge.getSetup();
      System.out.println("BridgeSetup : " + setup);
      //setup.setVeryVerbose(true);
      for (String key: config.stringPropertyNames()) {
        if (key.startsWith("AssemblyPath.")) {
          Bridge.LoadAndRegisterAssemblyFrom(new File (config.getString(key)));
          System.out.println("loaded assembly " + config.getString(key));
        } else if (key.startsWith("AssemblyPaths.")) {
          String[] split = RegexUtils.PIPE_PATTERN.split(config.getString(key));
          if ((split == null) || (split.length < 2)) continue;
          String[] paths = RegexUtils.SEMICOLUMN_PATTERN.split(split[1]);
          for (String path: paths) {
            Bridge.LoadAndRegisterAssemblyFrom(new File(split[0], path));
            System.out.printf("loaded assembly %s/%s%n", split[0], path);
          }
        }
      }
      JPPFConfiguration.getProperties().setBoolean("jppf.dotnet.bridge.initialized", true);
    } catch (Throwable t) {
      JPPFConfiguration.getProperties().setBoolean("jppf.dotnet.bridge.initialized", false);
      System.err.printf(".Net bridge initialization failure: %s%n", ExceptionUtils.getStackTrace(t));
    }
  }
}
