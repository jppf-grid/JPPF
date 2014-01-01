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
 * Instances of this class provide the computer idle time on a Mac system.
 * @author Laurent Cohen
 */
public class MacIdleTimeDetector implements IdleTimeDetector
{
  /**
   * Wraps the interactions with the native library.
   */
  public interface ApplicationServices extends Library
  {
    /**
     * Wrapper for the native library.
     */
    ApplicationServices INSTANCE = (ApplicationServices) Native.loadLibrary("ApplicationServices", ApplicationServices.class);
    /**
     * The type for any mouse or keyboard input event.
     */
    int KCG_ANY_INPUT_EVENT_TYPE = ~0;
    /**
     * User-only state.
     */
    int KCG_EVENT_SOURCE_STATE_PRIVATE = -1;
    /**
     * System-only state.
     */
    int KCG_EVENT_SOURCE_STATE_HID_SYSTEM_STATE = 1;
    /**
     * User and system state.
     */
    int KCG_EVENT_SOURCE_STATE_COMBINED_SESSION_STATE = 0;

    /**
     * Returns the elapsed time since the last event for a Quartz event source.
     * @param sourceStateId the source state to access.
     * @param eventType the event type to access. To get the elapsed time since the previous input event: keyboard, mouse, or tablet, specify KCG_ANY_INPUT_EVENT_TYPE.
     * @return the elapsed seconds since the last input event.
     * @see http://developer.apple.com/mac/library/documentation/Carbon/Reference/QuartzEventServicesRef/Reference/reference.html#//apple_ref/c/func/CGEventSourceSecondsSinceLastEventType
     */
    double CGEventSourceSecondsSinceLastEventType(int sourceStateId, int eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getIdleTimeMillis()
  {
    double idleTimeSeconds = ApplicationServices.INSTANCE.CGEventSourceSecondsSinceLastEventType(
        ApplicationServices.KCG_EVENT_SOURCE_STATE_COMBINED_SESSION_STATE, ApplicationServices.KCG_ANY_INPUT_EVENT_TYPE);
    return (long) (idleTimeSeconds * 1000);
  }
}
