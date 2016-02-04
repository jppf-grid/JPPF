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

package org.jppf.location;

import java.io.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.utils.streams.*;

/**
 * Instances of this class represent the location of an artifact, generally a file or the data found at a url.
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public abstract class AbstractLocation<T> implements Location<T> {
  /**
   * The path for this location.
   */
  protected final T path;
  /**
   * The list of listeners to this location.
   */
  protected final List<LocationEventListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Boolean flag that determines if at least one listener is registered.
   * Used to minimize the overhead of sending events if there is no listener.
   */
  protected boolean eventsEnabled = false;

  /**
   * Initialize this location with the specified type and path.
   * @param path the path for this location.
   */
  public AbstractLocation(final T path) {
    this.path = path;
  }

  @Override
  public T getPath() {
    return path;
  }

  @Override
  public Location copyTo(final Location<?> location) throws Exception {
    copyStream(getInputStream(), location.getOutputStream(), eventsEnabled);
    return location;
  }

  @Override
  public byte[] toByteArray() throws Exception {
    JPPFByteArrayOutputStream os = new JPPFByteArrayOutputStream();
    copyStream(getInputStream(), os, false);
    return os.toByteArray();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("path=").append(path);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public void addLocationEventListener(final LocationEventListener listener) {
    if (listener == null) throw new NullPointerException("null listener not accepted");
    listeners.add(listener);
    if (!eventsEnabled) eventsEnabled = true;
  }

  @Override
  public void removeLocationEventListener(final LocationEventListener listener) {
    if (listener == null) throw new IllegalArgumentException("null listener not accepted");
    listeners.remove(listener);
    if (listeners.isEmpty()) eventsEnabled = false;
  }

  /**
   * Notify all listeners that a data transfer has occurred.
   * @param n the size of the data that was transferred.
   */
  protected void fireLocationEvent(final long n) {
    if (listeners.isEmpty()) return;
    LocationEvent event = new LocationEvent(this, n);
    for (LocationEventListener l: listeners) l.dataTransferred(event);
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * @param is the input stream to read from.
   * @param os the output stream to write to.
   * @param withEvt if <code>true</code>, then {@link LocationEvent}s will be generated during the transfer.
   * @throws IOException if an I/O error occurs.
   */
  private void copyStream(final InputStream is, final OutputStream os, final boolean withEvt) throws IOException {
    OutputStream tmpos = !withEvt ? os : new NotifyingOutputStream(os, new NotifyingStreamCallback() {
      @Override
      public void bytesNotification(final long length) throws IOException {
        fireLocationEvent(length);
      }
    });
    StreamUtils.copyStream(is, tmpos, true);
  }
}
