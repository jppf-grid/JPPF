/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.server.nio.*;

/**
 * Channel wrapper and I/O implementation for the class loader of an in-VM node.
 * @author Laurent Cohen
 */
public class LocalClassLoaderChannel extends AbstractLocalChannelWrapper<JPPFResourceWrapper, AbstractNioContext>
{
  /**
   * Initialize this I/O handler with the specified context.
   * @param context the context used as communication channel.
   */
  public LocalClassLoaderChannel(final AbstractNioContext context)
  {
    super(context);
  }
}
