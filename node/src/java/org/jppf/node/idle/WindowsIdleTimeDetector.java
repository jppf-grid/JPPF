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

package org.jppf.node.idle;

import com.sun.jna.platform.win32.*;

/**
 * Instances of this class provide the computer idle time on a Windows system.
 * @author Laurent Cohen
 */
class WindowsIdleTimeDetector implements IdleTimeDetector {
  @Override
  public long getIdleTimeMillis() {
    WinUser.LASTINPUTINFO lastInputInfo = new WinUser.LASTINPUTINFO();
    User32.INSTANCE.GetLastInputInfo(lastInputInfo);
    return Kernel32.INSTANCE.GetTickCount() - lastInputInfo.dwTime;
  }
}
