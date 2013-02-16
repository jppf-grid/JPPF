/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.scheduler.bundle;

/**
 * Bundler implementations should implement this interface if they wish to have access to a node's context.
 * @author Martin JANDA
 */
public interface ContextAwareness
{
  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  JPPFContext getJPPFContext();
  /**
   * Set the corresponding node's context information. This method is called before <code>Bundler.setup()</code> with
   * context instance. And with <code>null</code> after <code>Bundler.dispose()</code>.
   * @param context a {@link JPPFContext} instance.
   */
  void setJPPFContext(JPPFContext context);
}
