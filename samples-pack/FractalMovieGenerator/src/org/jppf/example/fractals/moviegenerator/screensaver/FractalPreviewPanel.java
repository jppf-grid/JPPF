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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.jppf.example.fractals.FractalPoint;
import org.jppf.utils.FileUtils;

/**
 * This component displays a preview of the image being computed by the JPPF tasks.
 * @author Laurent Cohen
 */
public class FractalPreviewPanel extends JPanel
{
  /**
   * Holds the next points to draw.
   */
  private java.util.Queue<FractalPoint> queue = new ConcurrentLinkedQueue<>();
  /**
   * Set to {@code true} whenever the preview should be rest to its background color (erased).
   */
  private AtomicBoolean reset = new AtomicBoolean(false);
  /**
   * How much to scale down (or up) from the original image to the preview.
   */
  private double scale = 1d;
  /**
   * Size of rectangles drawn as points, computed based on the scale.
   */
  private int rectSize = 1;
  /**
   * Image used as buffer for painting. This is where the actual drawing takes place.
   */
  private final BufferedImage buffer;
  /**
   * Separate counter for the queue size, to avoid lengthy computations via {@code queue.size()}. 
   */
  private final AtomicInteger queueSize = new AtomicInteger(0);
  /**
   * A {@code Graphics} use to draw in the buffer image.
   */
  private final Graphics2D gb;

  /**
   * Initialize this component.
   */
  public FractalPreviewPanel() {
    super(true);
    setOpaque(true);
    setBackground(Color.BLACK);
    Dimension d = new Dimension(400, 400);
    setSize(d);
    setPreferredSize(d);
    setMinimumSize(d);
    setMaximumSize(d);
    BufferedImage bi = null;
    try (InputStream is = FileUtils.getFileInputStream("icons/mandelbrot.png")) {
      bi = ImageIO.read(is);
    } catch (Exception e) {
      e.printStackTrace();
    }
    buffer = (bi != null) ? bi : new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
    gb =  (Graphics2D) buffer.getGraphics();
  }

  @Override
  public void paint(final Graphics g) {
    boolean draw = false;
    if (reset.compareAndSet(true, false)) {
      gb.setColor(getBackground());
      gb.fillRect(0, 0, 400, 400);
      draw = true;
    }
    int size = queueSize.get();
    if (size > 0) {
      for (int i=0; i<size; i++) {
        FractalPoint point = queue.poll();
        if (point == null) break;
        queueSize.decrementAndGet();
        gb.setColor(new Color(point.rgb));
        gb.fillRect(point.x, 400 - point.y - 1, rectSize, rectSize);
      }
      draw = true;
    }
    //if (draw) g.drawImage(buffer, 0, 0, null);
    g.drawImage(buffer, 0, 0, null);
  }

  @Override
  public void paintComponent(final Graphics g) {
  }

  /**
   * Recompute the scale factor based on the specified image dimensions.
   * @param width the width of the original image.
   * @param height the height of the original image.
   */
  public void updateScaling(final int width, final int height) {
    double d1 = (double) getWidth() / (double) width;
    double d2 = (double) getHeight() / (double) height;
    scale = Math.min(d1, d2);
    rectSize = (int) Math.ceil(scale);
    if (rectSize < 1) rectSize = 1;
  }

  /**
   * Reset the preview to a black background.
   */
  public void doReset() {
    int size = queueSize.get();
    int count = 0;
    for (int i=0; i<size; i++) {
      if (queue.poll() == null) break;
      queueSize.decrementAndGet();
      count++;
    }
    reset.compareAndSet(false, true);
  }

  /**
   * Add a point to draw at the next repaint.
   * @param point the point to add.
   */
  public void addPoint(final FractalPoint point) {
    queue.offer(point.scale(scale));
    int size = queueSize.incrementAndGet();
  }
}
