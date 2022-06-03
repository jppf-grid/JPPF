/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.ui.options.event;

import java.awt.event.*;
import java.util.concurrent.*;

import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.options.xml.OptionsPageBuilder;

/**
 * This class performs cleanup and preferences storing actions when the admin console is closed.
 * @author Laurent Cohen
 */
public class WindowClosingListener extends WindowAdapter {
  @Override
  public void windowClosing(final WindowEvent event) {
    final CountDownLatch cdl = new CountDownLatch(1);
    new Thread(() -> {
      if (StatsHandler.hasInstance()) StatsHandler.getInstance().getClientHandler().close();
      final OptionElement elt = OptionsHandler.getTopPage();
      if (elt != null) {
        final OptionsPageBuilder builder = new OptionsPageBuilder();
        builder.triggerFinalEvents(elt);
      }
      OptionsHandler.savePreferences();
      cdl.countDown();
    }).start();
    try {
      cdl.await(3000L, TimeUnit.MILLISECONDS);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }
    System.exit(0);
  }
}
