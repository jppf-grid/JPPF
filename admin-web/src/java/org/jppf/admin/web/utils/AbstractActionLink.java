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

package org.jppf.admin.web.utils;

import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.*;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.AbstractJPPFPage;
import org.jppf.admin.web.tabletree.*;
import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractActionLink extends AjaxLink<String> {
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
    Pair<String, String> pair = FileUtils.getFileNameAndExtension(imageName);
    String format = "<img src='" + RequestCycle.get().getRequest().getContextPath() + "/images/toolbar/%s.%s'/>";
    if ((action != null) && (!action.isEnabled() || !action.isAuthorized())) {
      tag.getAttributes().put("class", "button_link_disabled");
      if (pair != null) setBody(Model.of(String.format(format, pair.first() + "-disabled", pair.second())));
    } else {
      if (pair != null) setBody(Model.of(String.format(format, pair.first(), pair.second())));
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
      AjaxSelfUpdatingTimerBehavior timer = ((RefreshTimerHolder) target.getPage()).getRefreshTimer();
      //if (debugEnabled) log.debug("stopping timer {} for page {}", timer, target.getPage());
      if (timer != null) timer.stop(null);
    }
  }

  /**
   * Restart the refresh timer, if any, on the page specified by the target.
   * @param target the Ajax target.
   */
  protected void restartRefreshTimer(final AjaxRequestTarget target) {
    //if (debugEnabled) log.debug("restarting timer for page {}", target.getPage());
    if (target.getPage() instanceof RefreshTimerHolder) {
      AjaxSelfUpdatingTimerBehavior timer = ((RefreshTimerHolder) target.getPage()).getRefreshTimer();
      //if (debugEnabled) log.debug("restarting timer {} for page {}", timer, target.getPage());
      if (timer != null) timer.restart(null);
    }
  }

  /**
   * Add the table tree of the page specified by the target, to the target.
   * @param target the Ajax target.
   */
  protected void addTableTreeToTarget(final AjaxRequestTarget target) {
    if (target.getPage() instanceof TableTreeHolder) {
      JPPFTableTree tableTree = ((TableTreeHolder) target.getPage()).getTableTree();
      if (tableTree != null) target.add(tableTree);
    }
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    if (tooltip) ((AbstractJPPFPage) getPage()).setTooltip(this);
  }
}
