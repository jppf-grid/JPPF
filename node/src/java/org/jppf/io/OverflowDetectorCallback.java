/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.io;

import java.io.IOException;

import org.jppf.utils.streams.NotifyingStreamCallback;

/**
 * This callback throws an IOException whenever the cumulated size of the data
 * written to a stream exceeds {@link java.lang.Integer#MAX_VALUE}.
 * @author Laurent Cohen
 */
public class OverflowDetectorCallback implements NotifyingStreamCallback
{
  /**
   * Maximum Integer expressed as a Long to ensure no implicit coversion has to be odne at runtime.
   */
  private static long MAX_VALUE = (long) Integer.MAX_VALUE;
  /**
   * The cumulated size of data written to the stream.
   */
  private long sum = 0L;

  @Override
  public void bytesNotification(final long length) throws IOException
  {
    sum += length;
    if (sum >= MAX_VALUE)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("stream output exceeds maximum size of Integer.MAX_VALUE, current size=");
      sb.append(sum - length).append(" bytes, about to add ").append(length).append(" bytes");
      throw new SerializationOverflowException(sb.toString());
    }
  }
}
