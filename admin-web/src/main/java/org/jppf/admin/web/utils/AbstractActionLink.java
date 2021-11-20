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

package org.jppf.admin.web.utils;

import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.*;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractActionLink extends AjaxLink<String> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractActionLink.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether this link is enabled and/or authorized.  
   */
  private transient UpdatableAction action;
  /**
   * Name of the associated icon.
   */
  protected String imageName;
  /**
   * Whether this link has an associated tooltip.
   */
  protected boolean tooltip = true;

  /**
   * @param id the link id.
   */
  public AbstractActionLink(final String id) {
    this(id, null, null);
  }

  /**
   * @param id the link id.
   * @param model the display model.
   */
  public AbstractActionLink(final String id, final IModel<String> model) {
    this(id, model, null);
  }

  /**
   * @param id the link id.
   * @param model the display model.
   * @param imageName the name of the associated icon.
   */
  public AbstractActionLink(final String id, final IModel<String> model, final String imageName) {
    super(id, model);
    this.imageName = imageName;
  }

  @Override
  protected void onComponentTag(final ComponentTag tag) {
    super.onComponentTag(tag);
    final Pair<String, String> pair = FileUtils.getFileNameAndExtension(imageName);
    final String contextPath = RequestCycle.get().getRequest().getContextPath();
    String imageKey = null;
    if ((action != null) && (!action.isEnabled() || !action.isAuthorized())) {
      tag.getAttributes().put("class", "button_link_disabled");
      if (pair != null) imageKey = pair.first() + "-disabled";
    } else {
      if (pair != null) imageKey = pair.first();
    }
    if (imageKey != null) {
      imageKey = "images/toolbar/" + imageKey + "." + pair.second();
      final String resourceURL = JPPFWebConsoleApplication.get().getSharedImageURL(imageKey);
      final String html = "<img src='" + contextPath + resourceURL + "'/>";
      setBody(Model.of(html));
      if (debugEnabled) log.debug("image html for key = {}, contextPath = {}: {}", imageKey, contextPath, html);
    }
    setEscapeModelStrings(false);
  }

  /**
   * @return the associated action.
   */
  public UpdatableAction getAction() {
    return action;
  }

  /**
   * @param action the associated action.
   */
  public void setAction(final UpdatableAction action) {
    this.action = action;
  }

  /**
   * Stop the refresh timer, if any, on the page specified by the target.
   * @param target the Ajax target.
   */
  protected void stopRefreshTimer(final AjaxRequestTarget target) {
    //if (debugEnabled) log.debug("stopping timer for page {}", target.getPage());
    if (target.getPage() instanceof RefreshTimerHolder) {
      final AjaxSelfUpdatingTimerBehavior timer = ((RefreshTimerHolder) target.getPage()).getRefreshTimer();
      //if (debugEnabled) log.debug("stopping timer {} for page {}", timer, target.getPage());
      if (timer != null) timer.stop(target);
    }
  }

  /**
   * Restart the refresh timer, if any, on the page specified by the target.
   * @param target the Ajax target.
   */
  protected void restartRefreshTimer(final AjaxRequestTarget target) {
    //if (debugEnabled) log.debug("restarting timer for page {}", target.getPage());
    if (target.getPage() instanceof RefreshTimerHolder) {
      final AjaxSelfUpdatingTimerBehavior timer = ((RefreshTimerHolder) target.getPage()).getRefreshTimer();
      //if (debugEnabled) log.debug("restarting timer {} for page {}", timer, target.getPage());
      if (timer != null) timer.restart(target);
    }
  }

  /**
   * Add the table tree of the page specified by the target, to the target.
   * @param target the Ajax target.
   */
  protected void addTableTreeToTarget(final AjaxRequestTarget target) {
    if (target.getPage() instanceof TableTreeHolder) {
      final JPPFTableTree tableTree = ((TableTreeHolder) target.getPage()).getTableTree();
      if (tableTree != null) target.add(tableTree);
    }
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    if (tooltip) ((AbstractJPPFPage) getPage()).setTooltip(this);
  }
}
