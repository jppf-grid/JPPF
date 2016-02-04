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

package org.jppf.node.screensaver.impl;

import java.awt.*;
import java.util.Random;

import javax.swing.ImageIcon;


/**
 * Data structure holding the position and direction of a moving logo.
 */
class ImageData {
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
  public int stepx = 1;
  /**
   * The direction on the y axis.
   */
  public int stepy = 1;
  /**
   * Width of the moving logo image.
   */
  public final int imgw;
  /**
   * Height of the moving logo image.
   */
  public final int imgh;
  /**
   * The logo image.
   */
  public Image img;
  /**
   * Used to generate random steps to avoid fixed paths around the screen.
   */
  private Random rand = new Random(System.nanoTime());

  /**
   * Initialize witht he specified image icon.
   * @param logoIcon an icon contain the imge of a logo.
   */
  public ImageData(final ImageIcon logoIcon) {
    img = logoIcon.getImage();
    imgw = logoIcon.getIconWidth();
    imgh = logoIcon.getIconHeight();
  }

  /**
   * Initialize this image data.
   * @param dim the dimension of the full screen or of the component the logos are painted in.
   */
  public void init(final Dimension dim) {
    int n = dim.width - imgw;
    if (n <= 0) n = imgw;
    prevx = x = rand.nextInt(n);
    stepx = randomValueInRange(-2, 2);
    n = dim.height - imgh;
    if (n <= 0) n = imgh;
    prevy = y = rand.nextInt(n);
    stepy = randomValueInRange(-2, 2);
  }

  /**
   * Determine whether this logo and another are colliding.
   * @param d the position and speed vector data for the otehr logo logo.
   * @return true if the two logos are colliding, false otherwise.
   */
  private boolean checkColliding(final ImageData d) {
    int x1 = x + stepx;
    int y1 = y + stepy;
    if (isIn(x1, y1, d)) {
      if (x >= d.x + d.imgw) reverseX(d);
      if (y >= d.y + d.imgh) reverseY(d);
      return true;
    }
    if (isIn(x1 + imgw, y1, d)) {
      if (x <= d.x - d.imgw) reverseX(d);
      if (y >= d.y + d.imgh) reverseY(d);
      return true;
    }
    if (isIn(x1, y1 + imgh, d)) {
      if (x >= d.x + d.imgw) reverseX(d);
      if (y <= d.y - d.imgh) reverseY(d);
      return true;
    }
    if (isIn(x1 + imgw, y1 + imgh, d)) {
      if (x <= d.x - d.imgw) reverseX(d);
      if (y <= d.y - d.imgh) reverseY(d);
      return true;
    }
    return false;
  }

  /**
   * Reverse the direction on the X axis of this image and the specified one, when they are colliding.
   * @param d the colliding image.
   */
  private void reverseX(final ImageData d) {
    stepx  = -stepx;
    d.stepx  = -d.stepx;
  }

  /**
   * Reverse the direction on the Y axis of this image and the specified one, when they are colliding.
   * @param d the colliding image.
   */
  private void reverseY(final ImageData d) {
    stepy  = -stepy;
    d.stepy  = -d.stepy;
  }

  /**
   * Update this image data by incrementing the position according to the direction vector.
   * @param screenDimension ther dimension of the full screen or of the component the logos are painted in.
   * @param data the array of other logos to check for collisions.
   * @param startIndex the index at which to start checking in the array of logos.
   */
  public synchronized void update(final Dimension screenDimension, final ImageData[] data, final int startIndex) {
    if (data != null) {
      for (int i=startIndex; i<data.length; i++) checkColliding(data[i]);
    }
    if ((x + stepx < 0) || (x + stepx + imgw > screenDimension.width)) stepx = -stepx;
    if ((y + stepy < 0) || (y + stepy + imgh > screenDimension.height)) stepy = -stepy;
    x += stepx;
    y += stepy;

    // change speed vector at random intervals
    int r = rand.nextInt(100);
    if (r == 0) {
      int s = stepx < 0 ? -1 : 1;
      stepx = s * randomValueInRange(1, 2);
      s = stepy < 0 ? -1 : 1;
      stepy = s * randomValueInRange(1, 2);
    }
  }

  /**
   * Determine whether a corner of a logo is inside another logo.
   * @param x1 x coordinate of the corner of the first logo.
   * @param y1 y coordinate of the corner of the first logo.
   * @param d the other logo.
   * @return true if the corner of the first is logo is inside the second, false otherwise.
   */
  private boolean isIn(final int x1, final int y1, final ImageData d) {
    int x2 = d.x + d.stepx;
    int y2 = d.y + d.stepy;
    return (x1 >= x2) && (x1 <= x2 + d.imgw) && (y1 >= y2) && (y1 <= y2 + d.imgh);
  }

  /**
   * Get a random int value in the specified range, with the exception of zero.
   * @param min range lower bound.
   * @param max range upper bound.
   * @return an int value.
   */
  private int randomValueInRange(final int min, final int max) {
    int result = 0;
    while (result == 0) {
      int diff = max - min;
      result = min + rand.nextInt(diff + 1);
    }
    return result;
  }
}
