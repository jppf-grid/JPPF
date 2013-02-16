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

package org.jppf.data.transform;

import java.io.*;

/**
 * This is the interface for arbitrary transformation and reverse-transformation of blocks of data that transit through the network.
 * <p>Among others, this permits encryption of the data, allowing a measure of security on the grid.
 * <p>Transformations are transparent to the JPPF user, as they are only applied just before data is sent to the network,
 * and right after it is read by the remote peer.
 * @see org.jppf.data.transform.JPPFDataTransformFactory
 * @author Laurent Cohen
 */
public interface JPPFDataTransform
{
  /**
   * Transform a block of data into another, transformed one.
   * This operation must be such that the result of unwrapping the data of the destination must be the same as the source data.
   * @param source the input stream of data to transform.
   * @param destination the stream into which the transformed data is written.
   * @throws Exception if any error occurs while transforming the data.
   */
  void wrap(InputStream source, OutputStream destination) throws Exception;

  /**
   * Transform a block of data into another, reverse-transformed one.
   * This method is the reverse operation with regards to {@link #wrap(java.io.InputStream, java.io.OutputStream)}.
   * This operation must be such that the result of <code>this.unwrap(this.wrap(data))</code> is equal to <code>data</code>.
   * @param source the input stream of data to reverse-transform.
   * @param destination the stream into which the reverse-transformed data is written.
   * @throws Exception if any error occurs while transforming the data.
   */
  void unwrap(InputStream source, OutputStream destination) throws Exception;
}
