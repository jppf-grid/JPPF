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

package org.jppf.utils.streams;

/**
 * Definition of important constants for stream I/O.
 * @author Laurent Cohen
 */
public final class StreamConstants
{
  /**
   * Size of temporary buffers used in I/O transfers.
   */
  public static final int TEMP_BUFFER_SIZE = 4 * 1024;
  /**
   * A definition of an empty byte array.
   */
  public static final byte[] EMPTY_BYTES = new byte[0];
}
