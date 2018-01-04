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

package org.jppf.admin.web.health.thresholds;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.health.HealthConstants;
import org.jppf.admin.web.utils.AbstractModalLink;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ThresholdsLink extends AbstractModalLink<ThresholdsForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ThresholdsLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public ThresholdsLink(final Form<String> form) {
    super(HealthConstants.THRESHOLDS_ACTION, Model.of("Thresholds"), "thresholds.gif", ThresholdsPage.class, form);
    modal.setInitialWidth(330);
    modal.setInitialHeight(255);
  }

  @Override
  protected ThresholdsForm createForm() {
    return new ThresholdsForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }
  
  /**
   * Called when the ok button is clicked.
   */
  private static void doOK() {
    JPPFWebSession.get().getHealthData().updateThresholds();
  }
}
