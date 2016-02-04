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

package org.jppf.node.screensaver;

import java.awt.*;

import javax.swing.JFrame;

/**
 * This extension of JFrale ensures that the frame is brought to the fore,
 * on top of any other window, when {@code toFront()} is called.
 * @author Laurent Cohen
 */
public class FocusedJFrame extends JFrame {
  /**
   * Use to avoid inifinite recursion in {@code toFront()}.
   */
  private boolean alreadyCalled = false;
  /**
   * Default initialization for this frame.
   * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
   */
  public FocusedJFrame() throws HeadlessException {
    super();
  }

  /**
   * Initialize this frame with the specified title.
   * @param title the title displayed in the frmae caption.
   * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
   */
  public FocusedJFrame(final String title) throws HeadlessException {
    super(title);
  }

  @Override
  public void setVisible(final boolean visible) {
    // make sure that frame is marked as not disposed if it is asked to be visible
    if (visible) {
      //setDisposed(false);
    }
    // let's handle visibility...
    if (!visible || !isVisible()) { // have to check this condition simply because super.setVisible(true) invokes toFront if frame was already visible
      super.setVisible(visible);
    }
    // ...and bring frame to the front.. in a strange and weird way
    if (visible) {
      int state = super.getExtendedState();
      state &= ~Frame.ICONIFIED;
      super.setExtendedState(state);
      super.setAlwaysOnTop(true);
      super.toFront();
      super.requestFocus();
      super.setAlwaysOnTop(false);
    }
  }

  @Override
  public void toFront() {
    if (alreadyCalled) return;
    try {
      alreadyCalled = true;
      super.setVisible(true);
      int state = super.getExtendedState();
      state &= ~Frame.ICONIFIED;
      super.setExtendedState(state);
      super.setAlwaysOnTop(true);
      super.toFront();
      super.requestFocus();
      super.setAlwaysOnTop(false);
    } finally {
      alreadyCalled = false;
    }
  }
}
