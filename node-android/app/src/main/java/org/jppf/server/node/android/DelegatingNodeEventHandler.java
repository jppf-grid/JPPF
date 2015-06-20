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
package org.jppf.server.node.android;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.jppf.android.R;
import org.jppf.android.activities.NodeEventHandler;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.TaskExecutionEvent;
import org.jppf.node.protocol.ClassPath;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.node.protocol.JobSLA;

/**
 * .
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
   *
   */
  private View view = null;

  /**
   * Initialize this event handler with the default delegate.
   */
  public DelegatingNodeEventHandler(final Activity activity) {
    setActivity(activity);
    setDelegate(new NodeEventHandler());
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
    AndroidNodeIntegrationAdapter adapter = getDelegate();
    if (adapter != null) adapter.taskExecuted(event);
  }

  @Override
  void setActivity(final Activity activity) {
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
    AndroidNodeIntegrationAdapter adapter = null;
    JobSLA sla = event.getJob().getSLA();
    ClassPath classpath = sla.getClassPath();
    AbstractJPPFClassLoader cl = event.getTaskClassLoader();
    JobMetadata meta = event.getJob().getMetadata();
    String s = meta.getParameter("jppf.node.integration.class");
    Log.v(LOG_TAG, "jobHeaderLoaded() event handler class = " + s);
    if (s != null) {
      try {
        Class<?> c = Class.forName(s, true, cl);
        adapter = (AndroidNodeIntegrationAdapter) c.newInstance();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (adapter != null) setDelegate(adapter);
  }

  /**
   * Remove the previous view if any, and set the new view if possible.
   */
  private void resetUI() {
    final ViewGroup group = (ViewGroup) activity.findViewById(R.id.main_layout);
    final View newView = delegate.getContentView();
    Log.v(LOG_TAG, "resetUI() newView = " + newView + ", current view = " + view + ", delegate.activity = " + delegate.getActivity());
    if (newView != null) activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (view != null) group.removeView(view);
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
      delegate.setActivity(activity);
      this.delegate = delegate;
      resetUI();
    }
  }
}
