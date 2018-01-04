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

package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class LocalClassLoaderConnection extends AbstractClassLoaderConnection<LocalClassLoaderChannel> {
  /**
   * Initialize this connection with the specified channel.
   * @param channel the local channel to use.
   */
  public LocalClassLoaderConnection(final LocalClassLoaderChannel channel) {
    this.channel = channel;
  }

  @Override
  public void init() throws Exception {
    lock.lock();
    try {
      if (initializing.compareAndSet(false, true)) {
        try {
          final ResourceRequestRunner rr = new LocalResourceRequest(channel);
          performCommonHandshake(rr);
          System.out.println(build(getClass().getSimpleName(), ": Reconnected to the class server"));
        } catch (final Exception e) {
          throw new RuntimeException(e);
        } finally {
          initializing.set(false);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    lock.lock();
    try {
      if (requestHandler != null) {
        requestHandler.close();
        requestHandler = null;
      }
    } finally {
      lock.unlock();
    }
  }
}
