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

import org.jppf.node.idle.IdleTimeDetector;

import com.sun.jna.*;

/**
 * Instances of this class provide the computer idle time on a Windows system.
 * @author Laurent Cohen
 */
public class WindowsIdleTimeDetector implements IdleTimeDetector
{
  /**
   * LastInputInfo structure definition.
   */
  public static class LastInputInfo extends Structure
  {
    /**
     * The size of this structure.
     */
    public short cbSize = 8;
    /**
     * The tick number of the last mouse/keyboard activity.
     */
    public int dwTime;
  }

  /**
   * Wrapper for JNI calls to the user32 Windows library.
   */
  public interface User32 extends Library
  {
    /**
     * Instance of the User32 library bindings.
     */
    User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);
    /**
     * Query the time of last activity.
     * @param info the structure in which the last activity time is stored.
     * @return BOOL return code.
     */
    int GetLastInputInfo(LastInputInfo info);
  }

  /**
   * Wrapper for JNI calls to the kernel32 Windows library.
   */
  public interface Kernel32 extends Library
  {
    /**
     * Wrapper for the native library.
     */
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    /**
     * Retrieves the number of milliseconds that have elapsed since the system was started.
     * @see http://msdn2.microsoft.com/en-us/library/ms724408.aspx
     * @return number of milliseconds that have elapsed since the system was started.
     */
    int GetTickCount();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getIdleTimeMillis()
  {
    LastInputInfo lastInputInfo = new LastInputInfo();
    User32.INSTANCE.GetLastInputInfo(lastInputInfo);
    return Kernel32.INSTANCE.GetTickCount() - lastInputInfo.dwTime;
  }
}
