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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.jppf.example.common.FractalPoint;
import org.jppf.example.fractals.mandelbrot.MandelbrotConfiguration;
import org.jppf.node.NodeRunner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This view queues notifications emitted from the mandelbrot tasks and uses their information
 * (x and y coordinates plus RGB color) to draw the fractal image while the tasks are executing.
 * @author Laurent Cohen
 */
public class FractalSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
  /**
   * Log tag for this class.
   */
  private static String LOG_TAG = FractalSurfaceView.class.getSimpleName();
  /**
   * The queue of points to draw.
   */
  private final LinkedBlockingQueue<FractalPoint> queue = new LinkedBlockingQueue<>();
  /**
   * Temporary queue where the main queue is drained periodically.
   */
  private final LinkedBlockingQueue<FractalPoint> tempQueue = new LinkedBlockingQueue<>();
  /**
   * The generated image configuration.
   */
  private MandelbrotConfiguration config;
  /**
   * The actual buffer for the image to draw.
   */
  private Bitmap bitmap;
  /**
   * Scale factor to convert betzeen Mandelbrot and view coordinates.
   */
  private double scale;
  /**
   * The thread which updates the view.
   */
  ViewThread thread = null;
  /**
   * Whether {@link #doDraw(Canvas)} should be called.
   */
  AtomicBoolean firstDraw = new AtomicBoolean(true);
  int currentWidth = 0;
  int currentHeight = 0;

  /**
   * Create this SurfaceView.
   * @param context the application context.
   */
  public FractalSurfaceView(Context context) {
    super(context);
    setWillNotDraw(false);
    getHolder().addCallback(this);
    Log.v(LOG_TAG, "FractalSurfaceView()");
  }

  @Override
  public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
    Log.v(LOG_TAG, "surfaceChanged() width=" + width + ", height=" + height);
    //stopViewThread();
    currentWidth = width;
    currentHeight = height;
    firstDraw.set(true);
    startViewThread();
  }

  @Override
  public void surfaceCreated(final SurfaceHolder holder) {
    Log.v(LOG_TAG, "surfaceCreated()");
    currentWidth = getWidth();
    currentHeight = getHeight();
  }

  @Override
  public void surfaceDestroyed(final SurfaceHolder holder) {
    Log.v(LOG_TAG, "surfaceDestroyed()");
    stopViewThread();
  }

  /**
   * Start the thread which updates the bitmap from the queue's content.
   */
  void startViewThread() {
    thread = new ViewThread();
    getBitmap();
    thread.start();
  }

  /**
   * Stop the thread which updates the bitmap from the queue's content.
   */
  void stopViewThread() {
    try {
      if (thread != null) {
        thread.stopped.set(true);
        thread.join();
        thread = null;
      }
    } catch(Exception e) {
    }
  }

  /**
   * Add a new point to the queue.
   * @param point the new point to draw on next invalidate().
   */
  public void addPoint(FractalPoint point) {
    queue.offer(point);
  }

  /**
   * Whether the queue is empty.
   * @return {@code true} if the queue is empty, {@code false} otherwise.
   */
  public boolean isQueueEmpty() {
    return queue.isEmpty();
  }

  /**
   * This is the draw method. It drains the queue of fractal points to a temporary queue,
   * then draws each of the points to the bitmap, then draws the bitmap onto the view's canvas.
   * @param canvas the view's canvas on which the bitmap is drawn.
   */
  private void doDraw(final Canvas canvas) {
    try {
      if (config != null) {
        // empty the points queue into a temporary queue
        queue.drainTo(tempQueue);
        boolean first = firstDraw.compareAndSet(true, false);
        if (!tempQueue.isEmpty() || first) {
          //Log.v(LOG_TAG, "doDraw() tempQueue size = " + tempQueue.size()+ ", view dimensions: width=" + getWidth() + ", height=" + getHeight());
          Bitmap bitmap = getBitmap();
          FractalPoint point = null;
          try {
            while ((point = tempQueue.poll()) != null) {
              int y = bitmap.getHeight() - (int) (point.y * scale) - 1;
              if (y >= bitmap.getHeight()) y = bitmap.getHeight();
              else if (y < 0) y = 0;
              // draw the pixel in the bitmap with full opacity
              bitmap.setPixel((int) (point.x * scale), y, point.rgb | 0xFF000000);
            }
          } catch(Exception e) {
            Log.e(LOG_TAG, "error in onDraw()", e);
          } finally {
            if (!tempQueue.isEmpty()) tempQueue.clear();
          }
          canvas.drawBitmap(bitmap, 0f, 0f, null);
        }
      } // else Log.v(LOG_TAG, "doDraw() tempQueue is empty, view dimensions: width=" + getWidth() + ", height=" + getHeight());
    } catch(Throwable t) {
      Log.e(LOG_TAG, "throwable in onDraw()", t);
    }
  }

  /**
   * Get the generated image configuration.
   * @param config the ocnfiguration containing the image dimensions.
   */
  public void setConfig(final MandelbrotConfiguration config) {
    this.config = config;
  }

  /**
   * Reset the bitmap.
   * @return A {@link Bitmap} object.
   */
  Bitmap resetBitmap() {
    NodeRunner.removePersistentData("fractals.bitmap");
    NodeRunner.removePersistentData("fractals.bitmap.scale");
    bitmap = null;
    return getBitmap();
  }

  /**
   * Get or create the bitmap in which the fractal is drawn.
   * @return A {@link Bitmap} object.
   */
  Bitmap getBitmap() {
    // if the bitmap is null, try to retrieve it from the node persistent data
    if (bitmap == null) {
      bitmap = (Bitmap) NodeRunner.getPersistentData("fractals.bitmap");
      Double d = (Double) NodeRunner.getPersistentData("fractals.bitmap.scale");
      if (d != null) scale = d;
    }
    if (bitmap == null) {
      if ((config != null) && (getWidth() > 0) && (getHeight() > 0)) {
        //Log.v(LOG_TAG, "getBitmap() creating new bitmap");
        // compute the scale factor
        scale = Math.min((double) currentWidth / config.width, (double) currentHeight / config.height);
        // in case the scale is > 1, we set it to 1 to avoid ugly graphics due to scaling
        if (scale > 1d) scale = 1d;
        bitmap = Bitmap.createBitmap((int) (config.width * scale), (int) (config.height * scale), Bitmap.Config.ARGB_8888);
        Log.v(LOG_TAG, "getBitmap() creating new bitmap " + bitmap);
        // persist the bitmap in the node persistent data for later reuse
        NodeRunner.setPersistentData("fractals.bitmap", bitmap);
        NodeRunner.setPersistentData("fractals.bitmap.scale", scale);
      }
    }
    return bitmap;
  }

  /**
   * This thread updates the view, checking the queue of points to draw periodically.
   */
  class ViewThread extends Thread {
    /**
     * Whether this thread is stopped.
     */
    AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    public void run() {
      try {
        while (!stopped.get()) {
          if (!queue.isEmpty() || firstDraw.get()) {
            SurfaceHolder holder = getHolder();
            if (holder != null) {
              Canvas canvas = null;
              try {
                canvas = holder.lockCanvas();
                synchronized(holder) {
                  doDraw(canvas);
                }
              } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
              }
            }
          } else {
            synchronized(this) {
              wait(100L);
            }
          }
        }
      } catch(Exception e) {
        Log.e(LOG_TAG, "Exception in ViewThread.run()", e);
      }
    }
  }
}
