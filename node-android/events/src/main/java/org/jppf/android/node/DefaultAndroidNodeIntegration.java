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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.jppf.android.events.R;
import org.jppf.node.NodeInternal;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This is the default node events handler, used if none is provided by the jobs.
 * @author Laurent Cohen
 * @since 5.1
 */
public class DefaultAndroidNodeIntegration extends AndroidNodeIntegrationAdapter {
  /**
   * Log tag for this class.
   */
  private final static String LOG_TAG = DefaultAndroidNodeIntegration.class.getSimpleName();
  /**
   * Atomic counter for the number of tasks executed by the node.
   */
  private static AtomicLong totalTasks = new AtomicLong(0);
  /**
   *
   */
  private View view = null;

  public DefaultAndroidNodeIntegration() {
    Log.v(LOG_TAG, "in DefaultAndroidNodeIntegration()");
  }

  @Override
  public View getContentView() {
    Log.v(LOG_TAG, "in getContentView() : view = " + view + ", activity = " + activity + ", this = " + this);
    if ((view == null) && (activity != null)) {
      LayoutInflater inflater = activity.getLayoutInflater();
      view = inflater.inflate(R.layout.default_view, null, false);
    }
    return view;
  }

  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    //if (event.isTaskCompletion()) totalTasks.incrementAndGet();
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "nodeStarting()");
    ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().addTaskExecutionListener(this);
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.current_job, "N/A");
        setViewText(R.id.node_state, activity.getString(R.string.node_state_online));
        setViewText(R.id.execution_state, activity.getString(R.string.execution_state_idle));
        setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
        setViewText(R.id.current_tasks, String.format("%,6d", 0));
      }
    });
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "nodeEnding()");
    ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().removeTaskExecutionListener(this);
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.node_state, activity.getString(R.string.node_state_offline));
      }
    });
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "starting job '" + event.getJob().getName() + "'");
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.current_job, event.getJob().getName());
        setViewText(R.id.node_state, activity.getString(R.string.node_state_offline));
        setViewText(R.id.execution_state, activity.getString(R.string.execution_state_executing));
        setViewText(R.id.current_tasks, String.format("%,6d", event.getTasks().size()));
        setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
      }
    });
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "ending job '" + event.getJob().getName() + "'");
    totalTasks.addAndGet(event.getTasks().size());
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.node_state, activity.getString(R.string.node_state_online));
        setViewText(R.id.execution_state, activity.getString(R.string.execution_state_idle));
        setViewText(R.id.total_tasks, String.format("%,6d", totalTasks.get()));
      }
    });
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
