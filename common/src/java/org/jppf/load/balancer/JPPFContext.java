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

package org.jppf.load.balancer;

/**
 * Bundler implementations should use this context if they wish to have access to execution context.
 * This context allows independence for bundlers to work in client and driver context.
 * @author Martin JANDA
 */
public abstract class JPPFContext
{
  /**
   * Get the maximum bundle size for the bundles present in the queue.
   * @return the bundle size as an int.
   */
  public abstract int getMaxBundleSize();
}
