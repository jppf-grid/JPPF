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

package org.jppf.ui.monitoring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.ui.monitoring.event.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ShowIPHandler {
  /**
   * List of listeners tot he state of og the ShowIP toggle registered with this stats handler.
   */
  private List<ShowIPListener> showIPListeners = new CopyOnWriteArrayList<>();
  /**
   * {@code true} to show IP addresses, {@code false} to display host names.
   */
  private AtomicBoolean showIP = new AtomicBoolean(false);

  /**
   * Register a <code>StatsHandlerListener</code> with this stats formatter.
   * @param listener the listener to register.
   */
  public void addShowIPListener(final ShowIPListener listener) {
    if (listener != null) showIPListeners.add(listener);
  }

  /**
   * Unregister a <code>StatsHandlerListener</code> from this stats formatter.
   * @param listener the listener to unregister.
   */
  public void removeShowIPListener(final ShowIPListener listener) {
    if (listener != null) showIPListeners.remove(listener);
  }

  /**
   * Determine whether to show IP addresses or host names.
   * @return {@code true} to show IP addresses, {@code false} to display host names.
   */
  public boolean isShowIP() {
    return showIP.get();
  }

  /**
   * Specify whether to show IP addresses or host names.
   * @param showIP {@code true} to show IP addresses, {@code false} to display host names.
   */
  public void setShowIP(final boolean showIP) {
    if (showIP != this.showIP.get()) {
      boolean oldState = this.showIP.get();
      this.showIP.set(showIP);
      ShowIPEvent event = new ShowIPEvent(this, oldState);
      for (ShowIPListener listener: showIPListeners) listener.stateChanged(event);
    }
  }
}
