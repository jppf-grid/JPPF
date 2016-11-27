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
package org.jppf.android.node;

import org.jppf.node.protocol.JPPFExceptionResult;
import org.jppf.node.protocol.JPPFExceptionResultEx;
import org.jppf.server.node.SerializationExceptionHook;

/**
 * This Android-specific hook implementation does not store the {@code Throwable} objects but instead describes it in a set of textual fields.
 * @author Laurent Cohen
 */
public class AndroidSerializationExceptionHook implements SerializationExceptionHook {
  @Override
  public JPPFExceptionResult buildExceptionResult(final Object o, final Throwable throwable) {
    return new JPPFExceptionResultEx(throwable, o);
  }
}
