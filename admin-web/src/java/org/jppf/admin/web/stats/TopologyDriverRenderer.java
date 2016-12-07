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

package org.jppf.admin.web.stats;

import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.jppf.client.monitoring.topology.TopologyDriver;

/**
 *
 * @author Laurent Cohen
 */
public class TopologyDriverRenderer implements IChoiceRenderer<TopologyDriver> {
  @Override
  public Object getDisplayValue(final TopologyDriver driver) {
    return driver.getDisplayName();
  }

  @Override
  public String getIdValue(final TopologyDriver driver, final int index) {
    return driver.getUuid();
  }

  @Override
  public TopologyDriver getObject(final String id, final IModel<? extends List<? extends TopologyDriver>> choices) {
    for (TopologyDriver driver: choices.getObject()) {
      if (id.equals(driver.getUuid())) return driver;
    }
    return null;
  }
}
