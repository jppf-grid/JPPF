/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.node.screensaver.impl;

import java.awt.Dimension;
import java.util.Random;

import javax.swing.ImageIcon;


/**
 * Data structure holding the position and direction of a flying logo.
 */
public class ImageData {
  /**
   * The previous position on the x axis.
   */
  public int prevx = 0;
  /**
   * The previous position on the y axis.
   */
  public int prevy = 0;
  /**
   * The position on the x axis.
   */
  public int x = 0;
  /**
   * The position on the y axis.
   */
  public int y = 0;
  /**
   * The direction on the x axis.
   */
  public int stepX = 1;
  /**
   * The direction on the y axis.
   */
  public int stepY = 1;
  /**
   * 
   */
  private final int imgw;
  /**
   * 
   */
  private final int imgh;
  /**
   * 
   */
  private Random rand = new Random(System.nanoTime());

  /**
   * Initialize witht he specified image icon.
   * @param logoIcon an icon contain the imge of a logo.
   */
  public ImageData(final ImageIcon logoIcon) {
    imgw = logoIcon.getIconWidth();
    imgh = logoIcon.getIconHeight();
  }

  /**
   * Determine whether this logo and another are colliding.
   * @param d the position and speed vector data for the otehr logo logo.
   * @return true if the two logos are colliding, false otherwise.
   */
  public boolean checkColliding(final ImageData d) {
    int x1 = x + stepX;
    int x2 = d.x + d.stepX;
    int y1 = y + stepY;
    int y2 = d.y + d.stepY;
    if (isIn(x1, y1, x2, y2)) {
      if (x >= d.x + imgw) {
        stepX  = -stepX;
        d.stepX  = -d.stepX;
      }
      if (y >= d.y + imgh) {
        stepY  = -stepY;
        d.stepY  = -d.stepY;
      }
      return true;
    }
    if (isIn(x1 + imgw, y1, x2, y2)) {
      if (x + imgw <= d.x) {
        stepX  = -stepX;
        d.stepX  = -d.stepX;
      }
      if (y >= d.y + imgh) {
        stepY  = -stepY;
        d.stepY  = -d.stepY;
      }
      return true;
    }
    if (isIn(x1, y1 + imgh, x2, y2)) {
      if (x >= d.x + imgw) {
        stepX  = -stepX;
        d.stepX  = -d.stepX;
      }
      if (y + imgh <= d.y) {
        stepY  = -stepY;
        d.stepY  = -d.stepY;
      }
      return true;
    }
    if (isIn(x1 + imgw, y1 + imgh, x2, y2)) {
      if (x + imgw <= d.x) {
        stepX  = -stepX;
        d.stepX  = -d.stepX;
      }
      if (y + imgh <= d.y) {
        stepY  = -stepY;
        d.stepY  = -d.stepY;
      }
      return true;
    }
    return false;
  }

  /**
   * Update this image data by incrementing the position according tot he direction vector.
   * @param screenDimension ther dimension of the full screen or of the component the logos are painted in.
   */
  public synchronized void update(final Dimension screenDimension) {
    int r = rand.nextInt(10);
    int n = 1 + (r == 0 ? 1 : 0);
    for (int i=0; i<n; i++) {
      if ((x + stepX < 0) || (x + stepX + imgw > screenDimension.width)) stepX = -stepX;
      if ((y + stepY < 0) || (y + stepY + imgh > screenDimension.height)) stepY = -stepY;
      x += stepX;
      y += stepY;
    }
  }

  /**
   * Determine whether a corner of a logo is inside another logo.
   * @param x1 x coordinate of the corner of the first logo.
   * @param y1 y coordinate of the corner of the first logo.
   * @param x2 x coordinate of the top left corner of the second logo.
   * @param y2 y coordinate of the top left corner of the second logo.
   * @return true if the corner of the first is logo is inside the second, false otherwise.
   */
  public boolean isIn(final int x1, final int y1, final int x2, final int y2) {
    return (x1 >= x2) && (x1 <= x2 + imgw) && (y1 >= y2) && (y1 <= y2 + imgh);
  }
}