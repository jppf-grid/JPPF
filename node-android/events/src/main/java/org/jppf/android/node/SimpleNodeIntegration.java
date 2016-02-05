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
package org.jppf.android.node;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;

/**
 * This is the default node events handler, used if none is provided by the jobs.
 * @author Laurent Cohen
 * @since 5.1
 */
public class SimpleNodeIntegration extends AndroidNodeIntegrationAdapter {
  // Atomic counter for the number of tasks executed by the node.
  private long totalTasks = 0L;
  // The view displayed by this feedback handler.
  private View view = null;

  @Override public View getContentView() {
    if ((view == null) && (getActivity() != null)) {
      view = new LinearLayout(getActivity()); // return an empty layout
    }
    return view;
  }

  @Override public void taskExecuted(final TaskExecutionEvent event) {
    Log.v("SimpleNodeIntegration", String.format("received event from task id '%s' : %s", event.getTask().getId(), event.getUserObject()));
  }

  @Override public void nodeStarting(final NodeLifeCycleEvent event) {
    Log.v("SimpleNodeIntegration", "the node is connected!");
  }

  @Override public void nodeEnding(final NodeLifeCycleEvent event) {
    Log.v("SimpleNodeIntegration", "the node is disconnected!");
  }

  @Override public void jobStarting(final NodeLifeCycleEvent event) {
    Log.v("SimpleNodeIntegration", "starting job '" + event.getJob().getName() + "'");
  }

  @Override public void jobEnding(final NodeLifeCycleEvent event) {
    Log.v("SimpleNodeIntegration", "ending job '" + event.getJob().getName() + "'");
    Log.v("SimpleNodeIntegration", "total tasks exectued: " + (totalTasks += event.getTasks().size()));
  }
}
