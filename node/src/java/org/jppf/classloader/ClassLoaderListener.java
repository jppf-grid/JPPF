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

package org.jppf.classloader;

import java.util.EventListener;

/**
 * Interface to implement for the objects that wish to receive notifications
 * of JPPF class loader events.
 * @author Laurent Cohen
 */
public interface ClassLoaderListener extends EventListener
{
  /**
   * Called when a class has been successfully loaded by a class loader.
   * @param event describes the class that was loaded.
   */
  void classLoaded(ClassLoaderEvent event);

  /**
   * Called when a class was not found by a class loader.
   * @param event describes the class that was not found.
   */
  void classNotFound(ClassLoaderEvent event);
}
