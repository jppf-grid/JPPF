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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.monitoring.node.TopologyData;
import org.slf4j.*;

/**
 * This action restarts a node.
 */
public class RestartNodeAction extends AbstractTopologyAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RestartNodeAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public RestartNodeAction()
  {
    setupIcon("/org/jppf/ui/resources/traffic_light_red_green.gif");
    setupNameAndTooltip("restart.node");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements)
  {
    super.updateState(selectedElements);
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event not used.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(final ActionEvent event)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        for (TopologyData data: dataArray)
        {
          try
          {
            JMXNodeConnectionWrapper node = (JMXNodeConnectionWrapper) data.getJmxWrapper();
            if (debugEnabled) log.debug("requesting restart of node " + node);
            node.restart();
          }
          catch(Exception e)
          {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    new Thread(r).start();
  }
}
