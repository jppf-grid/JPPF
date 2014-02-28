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

package org.jppf.example.fractals.moviegenerator.screensaver;

import org.jppf.example.fractals.FractalPoint;
import org.jppf.example.fractals.mandelbrot.MandelbrotConfiguration;
import org.jppf.node.event.*;
import org.jppf.node.screensaver.impl.NodeState;
import org.jppf.task.storage.DataProvider;

/**
 * This class extends the default screen saver's implementation to receive notifications from the tasks
 * during their execution, so as to update the image preview, along with task completion notifications
 * to update the rpogress bar.
 * @author Laurent Cohen
 */
public class FractalNodeIntegration extends NodeState {
  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    /*
    Task<?> task = event.getTask();
    if ((task instanceof MandelbrotTask) && (nodePanel != null)) {
      ((FractalPanel) nodePanel).getProgressPanel().incNbTasks();
    }
    */
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
    Object o = event.getUserObject();
    if ((o instanceof FractalPoint) && (nodePanel != null)) {
      ((FractalPanel) nodePanel).getFractalPreviewPanel().addPoint((FractalPoint) o);
    }
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    super.jobStarting(event);
    DataProvider dp = event.getDataProvider();
    Object o = dp.getParameter("config");
    if ((o instanceof MandelbrotConfiguration) && (nodePanel != null)) {
      FractalPreviewPanel fp = ((FractalPanel) nodePanel).getFractalPreviewPanel();
      fp.doReset();
      MandelbrotConfiguration cfg = (MandelbrotConfiguration) o;
      fp.updateScaling(cfg.width, cfg.height);
      //((FractalPanel) nodePanel).getProgressPanel().updateTotalTasks(event.getTasks().size());
    }
  }
}
