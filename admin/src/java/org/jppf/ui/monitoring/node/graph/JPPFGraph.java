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

package org.jppf.ui.monitoring.node.graph;

import org.slf4j.*;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFGraph extends mxGraph
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFGraph.class);
  /**
   * Determines whether trace log statements are enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Default constructor.
   */
  public JPPFGraph()
  {
    getSelectionModel().setSingleSelection(false);
    getSelectionModel().setEventsEnabled(true);
    setDisconnectOnMove(false);
    setCellsEditable(false);
    //setCellsResizable(false);
    setCellsSelectable(true);
    setAutoSizeCells(false);
    setConstrainChildren(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCellResizable(final Object cell)
  {
    boolean b = (cell instanceof mxCell) && ((mxCell) cell).isEdge();
    if (traceEnabled) log.trace("cell=" + cell + ", return=" + b);
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisconnectOnMove()
  {
    if (traceEnabled) log.trace("before return false");
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAutoSizeCell(final Object cell)
  {
    return isAutoSizeCells();
  }
}
