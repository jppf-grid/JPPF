/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.io.Serializable;

/**
 * This is a utility class to be used to store the pair of mean and the
 * number of samples this mean is based on.
 * @exclude
 */
public class PerformanceSample implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Mean compute time for server to node round trip.
   */
  public double mean;

  /**
   * Number of samples used to compute the mean value.
   */
  public long samples;

  /**
   * Default constructor.
   */
  public PerformanceSample() {
  }

  /**
   * Initialize this sample with the specified mean execute time and number of samples.
   * @param mean Mean compute time for server to node round trip.
   * @param samples Number of samples used to compute the mean value.
   */
  public PerformanceSample(final double mean, final long samples) {
    this.mean = mean;
    this.samples = samples;
  }
}
