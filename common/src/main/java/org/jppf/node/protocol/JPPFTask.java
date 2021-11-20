/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.node.protocol;

/**
 * This class is kept for compatibility with existing user installations which make use of it,
 * as we understand there are quite a few that still do. When creating new task types,
 * {@link AbstractTask} should always be used instead as a superclass.
 * @author Laurent Cohen
 */
public class JPPFTask extends AbstractTask<Object> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
}
