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

package org.jppf.server.protocol;

import org.jppf.node.protocol.JobMetadata;
import org.jppf.utils.collections.MetadataImpl;

/**
 * Instances of this class hold metadata about a job, that can be used from a load-balancer,
 * to adapt the load balancing to the computational weight of the job and/or the contained tasks.
 * It may be used in other places in future versions.
 * @see org.jppf.load.balancer.JobAwareness
 * @author Laurent Cohen
 */
public class JPPFJobMetadata extends MetadataImpl implements JobMetadata
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
}
