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

import org.jppf.node.event.DefaultLifeCycleErrorHandler;
import org.jppf.node.event.NodeIntegrationAdapter;
import org.jppf.node.event.NodeLifeCycleErrorHandler;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListener;
import org.jppf.utils.ExceptionUtils;

/**
 * .
 * @author Laurent Cohen
 */
public abstract class AndroidNodeIntegrationAdapter extends NodeIntegrationAdapter<Activity> implements NodeLifeCycleErrorHandler {
  /**
   * Log tag for this class.
   */
  private static String LOG_TAG = AndroidNodeIntegrationAdapter.class.getSimpleName();
  /**
   * An activity holding the UI to update.
   */
  protected Activity activity;

  /**
   * Get the view to be displayed during a job execution, if any.
   * @return a {@link View} which will replace the one in the main activity, or {@code null} to keep the existing view.
   */
  public abstract View getContentView();

  @Override
  public final void setUiComponent(final Activity uiComponent) {
    setActivity(uiComponent);
  }

  void setActivity(Activity activity) {
    this.activity = activity;
  }

  public Activity getActivity() {
    return activity;
  }

  @Override
  public void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    StringBuilder sb = new StringBuilder("error executing ");
    sb.append(event == null ? "unknown" : DefaultLifeCycleErrorHandler.methodsNamesMap.get(event.getType()));
    sb.append(" on an instance of ").append(listener == null ? "[unknown listener class]" : listener.getClass().getName());
    sb.append(", nbTasks=").append(event.getTasks() == null ? -1 : event.getTasks().size()).append(" :\n");
    Log.e(LOG_TAG, sb.append(ExceptionUtils.getStackTrace(t)).toString());
  }
}
