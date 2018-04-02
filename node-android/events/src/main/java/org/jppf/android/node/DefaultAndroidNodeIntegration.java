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
import android.widget.ImageView;
import android.widget.TextView;

import org.jppf.android.events.R;
import org.jppf.android.node.views.GlowingTextView;
import org.jppf.android.node.views.ProgressTextView;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;

import java.util.concurrent.atomic.AtomicInteger;
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
   * Format for the executed / job total field.
   */
  private static final String TASKS_FORMAT = "%,d / %,d";
  /**
   * Atomic counter for the number of tasks executed by the node.
   */
  private AtomicLong totalTasks = new AtomicLong(0);
  /**
   * The view handled by this node integration.
   */
  private View view = null;
  /**
   * The number of completed tasks from the current job, if any.
   */
  private AtomicInteger currentJobTasks = new AtomicInteger(0);
  /**
   * The number of tasks in the current job.
   */
  private int totalJobTasks = 0;

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
    final int jobCurrent = currentJobTasks.incrementAndGet();
    final int jobTotal = totalJobTasks;
    final long total = totalTasks.incrementAndGet();
    Log.v(LOG_TAG, String.format("taskExecuted() current=%d, total=%d, this=%s", jobCurrent, jobTotal, this));
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setCurrentTasksText(jobCurrent, jobTotal);
        setViewText(R.id.total_tasks, String.format("%,d", total));
      }
    });
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "nodeStarting()");
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.current_job, "N/A");
        setImageViewIcon(R.id.node_state, R.mipmap.on);
        setImageViewIcon(R.id.execution_state, R.mipmap.idle);
        setViewText(R.id.total_tasks, String.format("%,d", totalTasks.get()));
        setCurrentTasksText(0, 0);
      }
    });
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, "nodeEnding()");
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setImageViewIcon(R.id.node_state, R.mipmap.off);
      }
    });
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    final int total = totalJobTasks = event.getTasks().size();
    Log.v(LOG_TAG, String.format("starting job '%s' with %d tasks", event.getJob().getName(), totalJobTasks));
    currentJobTasks.set(0);
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setViewText(R.id.current_job, event.getJob().getName());
        setImageViewIcon(R.id.node_state, R.mipmap.off);
        setImageViewIcon(R.id.execution_state, R.mipmap.active);
        setCurrentTasksText(0, total);
        GlowingTextView currentJobView = (GlowingTextView) activity.findViewById(R.id.current_job);
        currentJobView.startAnimation(400L, currentJobView.getCurrentTextColor(), 0x808080);
      }
    });
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    Log.v(LOG_TAG, String.format("ending job '%s', currentTasks=%d, totalTasks=%d", event.getJob().getName(), currentJobTasks.get(), totalJobTasks));
    if (activity != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        setImageViewIcon(R.id.node_state, R.mipmap.on);
        setImageViewIcon(R.id.execution_state, R.mipmap.idle);
        setViewText(R.id.total_tasks, String.format("%,d", totalTasks.get()));
        GlowingTextView currentJobView = (GlowingTextView) activity.findViewById(R.id.current_job);
        currentJobView.endAnimation();
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

  /**
   * Set the specified image in the specified image view.
   * @param viewId the id of the text view to update.
   * @param iconId the id of the image resource to set.
   */
  private void setImageViewIcon(int viewId, int iconId) {
    ((ImageView) activity.findViewById(viewId)).setImageResource(iconId);
  }

  /**
   * Set the text of the "current tasks" text view.
   * @param current the number of executed tasks from the current jobs.
   * @param total the total number of tasks in the current jobs.
   */
  private void setCurrentTasksText(int current, int total) {
    int pct = total <= 0 ? 0 : (int) (100d * (double) current / (double) total);
    ((ProgressTextView) activity.findViewById(R.id.current_tasks)).setPct(pct);
    setViewText(R.id.current_tasks, String.format(TASKS_FORMAT, current, total));
  }
}
