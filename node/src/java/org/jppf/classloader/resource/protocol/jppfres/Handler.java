/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.io.IOException;
import java.net.*;

/**
 * URL stream handler for the &quot;jppfres:&quot; protocol, which handles class loader cache resources stored in memory.
 * @author Laurent Cohen
 */
public class Handler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(final URL u) throws IOException {
    return new JPPFResourceConnection(u);
  }

  @Override
  protected URLConnection openConnection(final URL u, final Proxy p) throws IOException {
    return openConnection(u);
  }

  /**
   * This method always returns <code>null</code>.
   * {@inheritDoc}
   */
  @Override
  protected synchronized InetAddress getHostAddress(final URL u) {
    return null;
  }
}
