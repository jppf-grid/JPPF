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

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.jppf.android.R;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;
import org.jppf.node.protocol.ClassPath;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.node.protocol.JobSLA;

/**
 * Delegates node events to another, dynamically loaded event handler, if any.
 * @author Laurent Cohen
 * @since 5.1
 */
public class DelegatingNodeEventHandler extends AndroidNodeIntegrationAdapter {
  /**
   * Log tag for this class.
   */
  private final static String LOG_TAG = DelegatingNodeEventHandler.class.getSimpleName();
  /**
   * The adapter to delegate notifications to.
   */
  private AndroidNodeIntegrationAdapter delegate;
  /**
   * The view managed by the delegate, if any.
   */
  private View view = null;
  /**
   * The UUID of the job being processed.
   */
  private String jobUuid;
  private boolean newJob = false;

  /**
   * Initialize this event handler with the default delegate.
   */
  public DelegatingNodeEventHandler(final Activity activity) {
    setActivity(activity);
    setDelegate(new DefaultAndroidNodeIntegration());
  }

  @Override
  public View getContentView() {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    return adapter == null ? null : adapter.getContentView();
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.taskNotification(event);
  }

  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    Log.v(LOG_TAG, "taskExecuted() this = " + this);
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.taskExecuted(event);
  }

  @Override
  public void setActivity(final Activity activity) {
    super.setActivity(activity);
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.setActivity(activity);
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.nodeStarting(event);
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.nodeEnding(event);
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    String newJobUuid = event.getJob().getUuid();
    newJob = (jobUuid == null) || !jobUuid.equals(newJobUuid);
    jobUuid = newJobUuid;
    AndroidNodeIntegrationAdapter adapter = null;
    if (newJob) {
      JobSLA sla = event.getJob().getSLA();
      ClassPath classpath = sla.getClassPath();
      AbstractJPPFClassLoader cl = null;
      Log.v(LOG_TAG, String.format("jobHeaderLoaded(job='%s') classpath=%s", event.getJob().getName(), classpath));
      if ((classpath != null) && !classpath.isEmpty()) cl = event.getNode().resetTaskClassLoader(classpath);
      else cl = event.getTaskClassLoader();
      JobMetadata metadata = event.getJob().getMetadata();
      String s = metadata.getParameter("jppf.node.integration.class");
      Log.v(LOG_TAG, "jobHeaderLoaded() event handler class = " + s);
      if (s != null) {
        try {
          Class<?> c = Class.forName(s, true, cl);
          adapter = (AndroidNodeIntegrationAdapter) c.newInstance();
        } catch (Exception e) {
          Log.e(LOG_TAG, "error instantiating the node event handler '" + s + "' : ", e);
        }
      }
      if (adapter == null) adapter = (delegate instanceof DefaultAndroidNodeIntegration) ? delegate : new DefaultAndroidNodeIntegration();
      setDelegate(adapter);
      adapter.jobHeaderLoaded(event);
    }
  }

  /**
   * Remove the previous view if any, and set the new view if possible.
   */
  public void resetUI() {
    if (delegate == null) return;
    final ViewGroup group = (ViewGroup) activity.findViewById(R.id.main_layout);
    final View newView = delegate.getContentView();
    Log.v(LOG_TAG, "resetUI() newView = " + newView + ", current view = " + view + ", delegate.activity = " + delegate.activity);
    if (newView != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        group.removeAllViews();
        if ((view != null) && (view.getParent() != null)) ((ViewGroup) view.getParent()).removeView(view);
        if (newView.getParent() == group) group.removeView(newView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        group.addView(newView, params);
        view = newView;
      }
    });
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.jobStarting(event);
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.jobEnding(event);
  }

  @Override
  public void beforeNextJob(final NodeLifeCycleEvent event) {
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.beforeNextJob(event);
  }

  /**
   * Whether the current job is the same job (same uuid) as the previous one.
   * @return true if the current job is different from the previous one, false otherwise.
   */
  public boolean isNewJob() {
    return newJob;
  }

  /**
   * Get the adapter to delegate notifications to.
   * @return an instance of {@link AndroidNodeIntegrationAdapter}.
   */
  synchronized AndroidNodeIntegrationAdapter getDelegate() {
    return delegate;
  }

  /**
   * Set the adapter to delegate notifications to.
   * @param delegate an instance of {@link AndroidNodeIntegrationAdapter}.
   */
  synchronized void setDelegate(final AndroidNodeIntegrationAdapter delegate) {
    Log.v(LOG_TAG, String.format("setDelegate() : delegate=%s, view=%s, activity=%s", delegate, view, activity));
    if ((delegate != null) && (delegate != this.delegate)) {
      if (this.delegate != null) this.delegate.setActivity(null);
      delegate.setActivity(activity);
      this.delegate = delegate;
      resetUI();
    }
  }
}
