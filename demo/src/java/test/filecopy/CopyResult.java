/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.filecopy;

import java.io.Serializable;

/**
 * This class encapsulates the results of reading a file chunk.
 * @author Laurent Cohen
 */
public class CopyResult implements Serializable {
  /**
   * The bytes read.
   */
  private final byte[] bytes;
  /**
   * Whether the copy is done.
   */
  private final boolean done;

  /**
   * 
   * @param bytes the bytes read.
   * @param done whether the copy is done.
   */
  public CopyResult(final byte[] bytes, final boolean done) {
    this.bytes = bytes;
    this.done = done;
  }

  /**
   * Get the bytes read.
   * @return an array of {@code byte}s, may be {@code null} if an exception occurred or EOF was reached.
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * Determine whether the copy is done. The copy is odne whenever it completed successfully or with exception.
   * @return {@code true} if the copy is done, {@code false} otherwise.
   */
  public boolean isDone() {
    return done;
  }
}
