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

package org.jppf.example.fractals;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

/**
 * This class is an extension of JPanel whose goal is to draw an image in its component area.
 * @author Laurent Cohen
 */
public class ImagePanel extends JPanel
{
  /**
   * The image to draw in this panel.
   */
  private transient BufferedImage image = null;

  /**
   * Default constructor.
   */
  public ImagePanel()
  {
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
  }

  /**
   * Paints this panel with a background image.<br/>
   * {@inheritDoc}
   */
  @Override
  protected void paintComponent(final Graphics g)
  {
    // Now call the superclass behavior to paint the foreground.
    super.paintComponent(g);
    if (image != null) g.drawImage(image, 0, 0, null);
  }

  /**
   * Get the image to draw in this panel.
   * @return an <code>Image</code> instance.
   */
  public Image getImage()
  {
    return image;
  }

  /**
   * Set the image to draw in this panel.
   * @param image an {@code BufferedImage} instance.
   */
  public void setImage(final BufferedImage image)
  {
    this.image = image;
    updateSize();
  }

  /**
   * Set the image to draw in this panel, specified as a file to load.
   * @param imagePath a string specifying the path ot an image file.
   * @throws Exception if the image could not be loaded.
   */
  public void setImage(final String imagePath) throws Exception
  {
    this.image = ImageIO.read(new File(imagePath));
    updateSize();
  }
  
  /**
   * 
   */
  private void updateSize()
  {
    Insets ins = getBorder().getBorderInsets(this);
    Dimension d = new Dimension(image.getWidth() + ins.left + ins.right, image.getHeight() + ins.bottom + ins.top);
    setSize(d);
    setPreferredSize(d);
    setMaximumSize(d);
    repaint();
  }
}
