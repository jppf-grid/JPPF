/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.classloader.resource.protocol.jppfres;

import java.io.*;
import java.net.*;
import java.util.List;

import org.jppf.classloader.resource.ResourceCache;
import org.jppf.location.Location;

/**
 * Implementation of a {@link URLConnection} for the &quot;jppfres:&quot; URL protocol.
 * @author Laurent Cohen
 */
public class JPPFResourceConnection extends URLConnection {
  /**
   * The class loader resource the URL points to.
   */
  private Location resource = null;
  /**
   * When true, indicates that a connection was already attempted and failed.
   */
  private boolean connectionFailed = false;

  /**
   * Create a new connection from the specified url.
   * @param url the url to use.
   */
  public JPPFResourceConnection(final URL url) {
    super(url);
  }

  @Override
  public void connect() throws IOException {
    try {
      ResourceCache rc = ResourceCache.getCacheInstance(url.getHost());
      StringBuilder path = new StringBuilder(url.getPath());
      char c;
      while (((c = path.charAt(0)) == '/') || (c == '\\')) path.deleteCharAt(0);
      String[] keyvalue = url.getQuery().split("\\?|=");
      int id = Integer.valueOf(keyvalue[1]);
      List<Location> list = rc.getResourcesLocations(path.toString());
      if (list != null) resource = list.get(id);
      else throw new IOException("URL '" + url + "' does not point to an existing or valid resource");
      this.connected = true;
    } catch (Exception e) {
      connectionFailed = true;
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return (InputStream) createStream(true);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return (OutputStream) createStream(false);
  }

  /**
   * Create a stream of the specified type for this connection.
   * @param isInputStream if <code>true</code> then return an <code>InputStream</code>, otherwise return an <code>OutputStream</code>.
   * @return the created stream.
   * @throws IOException if the connection fails or a previous connection attempt had failed.
   */
  private Object createStream(final boolean isInputStream) throws IOException {
    checkValid();
    try {
      return isInputStream ? resource.getInputStream() : resource.getOutputStream();
    } catch (Exception e) {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
  }

  /**
   * Performs the connection if it hasn't been attempted yet.
   * @throws IOException if the connection fails or a previous connection attempt had failed.
   */
  private void checkValid() throws IOException {
    if (!connected) {
      if (connectionFailed) throw new IOException("URL '" + url + "' does not point to an existing or valid resource");
      connect();
    }
    if (resource == null) throw new IOException("URL '" + url + "' does not point to an existing or valid resource");
  }
}
