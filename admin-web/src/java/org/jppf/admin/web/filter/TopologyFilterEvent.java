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

package org.jppf.admin.web.filter;

import java.util.EventObject;

/**
 * 
 * @author Laurent Cohen
 */
public class TopologyFilterEvent extends EventObject {
  /**
   * Initialize with the psecified filter as source.
   * @param filter the source of ths event.
   */
  public  TopologyFilterEvent(final TopologyFilter filter) {
    super(filter);
  }

  /**
   * @return the source of ths event.
   */
  public TopologyFilter getFilter() {
    return (TopologyFilter) getSource();
  }
}
