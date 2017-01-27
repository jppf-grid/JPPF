/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
package org.jppf.android.demo;

import android.util.Log;
import android.view.View;

import org.jppf.example.common.FractalPoint;
import org.jppf.example.fractals.mandelbrot.MandelbrotConfiguration;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListener;
import org.jppf.node.event.TaskExecutionEvent;
import org.jppf.node.protocol.DataProvider;
import org.jppf.android.node.AndroidNodeIntegrationAdapter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This event handler listens for notifications from the Mandelbrot tasks, each notification
 * representing a pixel of the fractal to draw. Pixels are drawn into a {@code Bitmap} which is used as buffer.
 * The view is actually queuing the notifications and processes them asynchronously in a separate thread.
 * @author Laurent Cohen
 */
public class FractalEventHandler extends AndroidNodeIntegrationAdapter {
  /**
   * Log tag for this class.
   */
  private static String LOG_TAG = FractalEventHandler.class.getSimpleName();
  /**
   * The view on which to draw the Mandelbrot fractal.
   */
  private FractalSurfaceView view = null;
  /**
   * Number of point notifications received.
   */
  private final AtomicInteger pointCount = new AtomicInteger(0);
  /**
   * The mandelbrot configuration as given in the job's data provider.
   */
  private MandelbrotConfiguration cfg = null;
  /**
   * The uuid of the current job, if any. Used to compare with the next job's uuid.
   */
  private String jobUuid = null;

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    pointCount.set(0);
    int nbTasks = event.getTasks().size();
    Log.v(LOG_TAG, String.format("jobStarting('%s') nbTasks=%d", event.getJob().getName(), nbTasks));
    // retrieve the fractal configuration from the job's data provider
    DataProvider dp = event.getDataProvider();
    cfg = dp.getParameter("config");
    FractalSurfaceView view = (FractalSurfaceView) getContentView();
    view.setConfig(cfg);
    // if this is not a continuation (dispatch) of the same job then ensure the bitmap is reset
    if ((jobUuid == null) || !jobUuid.equals(event.getJob().getUuid())) view.resetBitmap();
    jobUuid = event.getJob().getUuid();
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    int nbTasks = event.getTasks().size();
    Log.v(LOG_TAG, String.format("jobEnding('%s') nbTasks=%d", event.getJob().getName(), nbTasks));
    FractalSurfaceView.ViewThread thread = ((FractalSurfaceView) getContentView()).thread;
    // now that the job is completed, wait until the notifications queue is empty (i.e. until all points have been drawn) then stop the updating thread
    // this is to ensure the image is not missing any area
    if (thread != null) {
      try {
        FractalSurfaceView v = (FractalSurfaceView) getContentView();
        while (!v.isQueueEmpty() || (pointCount.get() < nbTasks * cfg.width)) Thread.sleep(1L);
        //v.stopViewThread();
      } catch(Exception e) {
      }
    }
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
    // get the notification as a point to draw on the view and add it to the queue
    FractalPoint point = (FractalPoint) event.getUserObject();
    ((FractalSurfaceView) getContentView()).addPoint(point);
    // diplay a log message every 10,000 points
    int n = pointCount.incrementAndGet();
    if (n % 10_000 == 0) Log.v(LOG_TAG, String.format("got %,d notifications", n));
  }

  @Override
  public View getContentView() {
    synchronized(this) {
      if (view == null) {
        final AtomicBoolean done = new AtomicBoolean(false);
        // create the view on the UI thread
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            view = new FractalSurfaceView(getActivity());
            done.set(true);
          }
        });
        // wait until the view is created
        try {
          while (!done.get()) Thread.sleep(1L);
        } catch(Exception e) {
        }
        Log.v(LOG_TAG, "getContentView() view created: " + view);
      }
    }
    return view;
  }

  @Override
  public void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    String s = String.format("error executing %s on an instance of %s for job '%s'", event.getType(), listener.getClass(), event.getJob().getName());
    Log.e(LOG_TAG, s, t);
  }
}
