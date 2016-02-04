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

package org.jppf.utils.streams;

import java.io.IOException;

/**
 * This interface defines a callback used by notifying streams to provide notifications
 * of the bytes read, written or skipped.
 * @see org.jppf.utils.streams.NotifyingInputStream
 * @see org.jppf.utils.streams.NotifyingOutputStream
 * @author Laurent Cohen
 */
public interface NotifyingStreamCallback
{
  /**
   * Notify that some bytes were read, written or skipped.
   * @param length the number of bytes.
   * @throws IOException if any I/O error occurs.
   */
  void bytesNotification(long length) throws IOException;
}
