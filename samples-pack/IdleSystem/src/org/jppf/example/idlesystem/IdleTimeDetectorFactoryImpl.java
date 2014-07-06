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

package org.jppf.example.idlesystem;

import org.jppf.node.idle.*;
import org.jppf.utils.SystemUtils;

/**
 * A factory implementation that returns an idle system detector based on the OS detected for the current host.
 * @author Laurent Cohen
 */
public class IdleTimeDetectorFactoryImpl implements IdleTimeDetectorFactory
{
  /**
   * {@inheritDoc}
   */
  @Override
  public IdleTimeDetector newIdleTimeDetector()
  {
    if (SystemUtils.isWindows()) return new WindowsIdleTimeDetector();
    else if (SystemUtils.isX11()) return new X11IdleTimeDetector();
    else if (SystemUtils.isMac()) return new MacIdleTimeDetector();
    return null;
  }
}
