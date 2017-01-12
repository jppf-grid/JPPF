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

package org.jppf.admin.web;

import org.apache.wicket.*;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;

/**
 *
 * @author Laurent Cohen
 */
public class AbstractJPPFPage extends WebPage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFPage.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Options for tooltips.
   */
  private static final Options TOOLTIP_OPTIONS = new Options();
  static {
    TOOLTIP_OPTIONS.set("position", "{ my: 'center top+3', at: 'center bottom' }");
    TOOLTIP_OPTIONS.set("track", false);
    TOOLTIP_OPTIONS.set("classes", "{ 'ui-tooltip': 'ui-corner-all jppf-tooltip' }");
    TOOLTIP_OPTIONS.set("show", "{ delay: '500', duration: '2000' }");
    //TOOLTIP_OPTIONS.set("hide", "{ delay: '3000' }");
  }
  /**
   * Prefix for the mount path of all pages.
   */
  public static final String PATH_PREFIX = "";

  /**
   *
   */
  public AbstractJPPFPage() {
    add(new TooltipBehavior(TOOLTIP_OPTIONS));
  }

  /**
   * Add a tooltip to the specified component.
   * @param <T> the type of the component.
   * @param comp the component on which to set a tooltip.
   * @return the component itself.
   */
  public <T extends Component> T setTooltip(final T comp) {
    String base = getClass().getName();
    String id = comp.getId();
    String key = null;
    String[] possibleEndings = { ".field", ".label" };
    for (String ending: possibleEndings) {
      if (id.endsWith(ending)) {
        int idx = id.lastIndexOf(ending);
        key = id.substring(0, idx) + ".tooltip";
        break;
      }
    }
    if (key == null) key = id + ".tooltip";
    comp.add(new AttributeModifier("title", LocalizationUtils.getLocalized(base, key, JPPFWebSession.get().getLocale())));
    return comp;
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();
    if (debugEnabled) log.debug("in onConfigure() for page {} ==> '{}'", getClass().getSimpleName(), RequestCycle.get().urlFor(getClass(), null));
    if (!JPPFWebSession.get().isSignedIn()) JPPFWebConsoleApplication.get().restartResponseAtSignInPage();
  }
}
