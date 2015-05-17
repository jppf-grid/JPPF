/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package org.jppf.android.activities;

import android.app.Activity;
import android.widget.TextView;

import org.jppf.android.R;
import org.jppf.node.NodeInternal;
import org.jppf.node.event.NodeIntegrationAdapter;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * .
 * @author Laurent Cohen
 */
public class NodeEventHandler extends NodeIntegrationAdapter<Activity> {
  /**
   * An activity holding the UI to update.
   */
  private Activity activity = null;
  private static AtomicLong totalTasks = new AtomicLong(0);

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
  }

  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    totalTasks.incrementAndGet();
  }

  @Override
  public void setUiComponent(final Activity uiComponent) {
    this.activity = uiComponent;
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().addTaskExecutionListener(this);
    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setViewText(R.id.node_state, "online");
          setViewText(R.id.execution_state, activity.getString(R.string.execution_state_idle));
          setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
          setViewText(R.id.current_tasks, String.format("%,6d", 0));
        }
      });
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().removeTaskExecutionListener(this);
    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setViewText(R.id.node_state, "offline");
        }
      });
    }
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setViewText(R.id.current_job, event.getJob().getName());
          setViewText(R.id.execution_state, activity.getString(R.string.execution_state_executing));
          setViewText(R.id.current_tasks, String.format("%,6d", event.getTasks().size()));
          setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
        }
      });
    }
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setViewText(R.id.execution_state, activity.getString(R.string.execution_state_idle));
          setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
        }
      });
    }
  }

  /**
   * Set the specified text in the specified text view.
   * @param viewId the id of the text view to update.
   * @param text the text to set in the view.
   */
  private void setViewText(int viewId, String text) {
    ((TextView) activity.findViewById(viewId)).setText(text);
  }
}
