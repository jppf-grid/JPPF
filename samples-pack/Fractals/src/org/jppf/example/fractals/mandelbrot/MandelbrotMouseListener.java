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

package org.jppf.example.fractals.mandelbrot;

import java.awt.Component;
import java.awt.event.MouseEvent;

import org.jppf.example.fractals.ImagePanel;
import org.jppf.example.fractals.RunnerFactory;
import org.jppf.ui.options.*;
import org.jppf.ui.options.JavaOption.JavaOptionMouseListener;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

/**
 * Mouse listener used in the Mandelbrot panel to perform the mouse-driven
 * zooming functionality.
 * @author Laurent Cohen
 */
public class MandelbrotMouseListener extends JavaOptionMouseListener {
  /**
   * Processes left-click and right-click events to zoom-in or zoom-out the image.
   * @param event the mouse event to process
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(final MouseEvent event) {
    Component comp = event.getComponent();
    if (!(comp instanceof ImagePanel)) return;

    int button = event.getButton();
    if ((button == MouseEvent.BUTTON1) || (button == MouseEvent.BUTTON3)) {
      int mouseX = event.getX();
      int mouseY = event.getY();
      double centerX = getDoubleValue("/centerX");
      double centerY = getDoubleValue("/centerY");
      double d = getDoubleValue("/diameter");
      TypedProperties jppfConfig = JPPFConfiguration.getProperties();
      int width = jppfConfig.getInt("image.width", 800);
      int height = jppfConfig.getInt("image.height", 600);
      double minX = centerX - d/2;
      double x = mouseX * d /(double) width + minX;
      double minY = centerY - d/2;
      double y = (height - mouseY - 1) * d / (double) height + minY;
      int f = getIntValue("/mandelbrotZoomFactor");

      d = (button == MouseEvent.BUTTON1) ? d/f : d * f;
      setDoubleValue("/centerX", x);
      setDoubleValue("/centerY", y);
      setDoubleValue("/diameter", d);
      int iter = getIntValue("/iterations");
      try {
        RunnerFactory.getRunner("mandelbrot").submitExecution(new MandelbrotConfiguration(x, y, d, iter));
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Get the value of the option with the specified name as a double.
   * @param name the name of the option to look for.
   * @return a double value.
   */
  protected double getDoubleValue(final String name)
  {
    Option o = (Option) option.findFirstWithName(name);
    return ((Number) o.getValue()).doubleValue();
  }

  /**
   * Set the value of the option with the specified name as a double.
   * @param name the name of the option to look for.
   * @param value the value to set as a double.
   */
  protected void setDoubleValue(final String name, final double value)
  {
    AbstractOption o = (AbstractOption) option.findFirstWithName("/"+name);
    o.setValue(value);
  }

  /**
   * Get the value of the option with the specified name as an int.
   * @param name the name of the option to look for.
   * @return an int value.
   */
  protected int getIntValue(final String name)
  {
    Option o = (Option) option.findFirstWithName("/"+name);
    return ((Number) o.getValue()).intValue();
  }

  /**
   * Set the value of the option with the specified name as a int.
   * @param name the name of the option to look for.
   * @param value the value to set as an int.
   */
  protected void setIntValue(final String name, final int value)
  {
    AbstractOption o = (AbstractOption) option.findFirstWithName("/"+name);
    o.setValue(value);
  }
}
