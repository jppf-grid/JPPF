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

package org.jppf.example.fractals.moviegenerator.screensaver;

import java.awt.*;

import javax.swing.*;

import org.jppf.node.screensaver.impl.NodePanel;

/**
 * Extends the default screensaver's implemntation to replace the static logo image
 * with a panel showing a preview of the image / movie frame being computed, along
 * with a progress bar showing how many tasks have comp^leted among those in the dispatched job.
 * @author Laurent Cohen
 */
public class FractalPanel extends NodePanel {
  /**
   * The fractal preview panel.
   */
  private FractalPreviewPanel fractalPreviewPanel;
  /**
   * The progress bar displayed under the preview.
   */
  private FractalProgressPanel progressPanel;

  /**
   * Creates a fractal-specific panel that replaces the JPPF@home image.<br/>
   * {@inheritDoc}
   */
  @Override
  protected JComponent createTopPanel() {
    JPanel panel = new JPanel();
    panel.setBackground(Color.BLACK);
    panel.setOpaque(false);
    GridBagLayout g = new GridBagLayout();
    panel.setLayout(g);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    addLayoutComp(panel, g, c, getFractalPreviewPanel());
    addLayoutComp(panel, g, c, Box.createVerticalStrut(5));
    return panel;
  }

  /**
   * Get the fractal preview panel.
   * @return a {@link FractalPreviewPanel} instance.
   */
  public FractalPreviewPanel getFractalPreviewPanel() {
    synchronized(this) {
      if (fractalPreviewPanel == null) {
        try {
          fractalPreviewPanel = new FractalPreviewPanel();
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
    return fractalPreviewPanel;
  }

  /**
   * The progress bar displayed under the preview.
   * @return a {@link FractalProgressPanel} instance.
   */
  public FractalProgressPanel getProgressPanel() {
    return progressPanel;
  }
}
