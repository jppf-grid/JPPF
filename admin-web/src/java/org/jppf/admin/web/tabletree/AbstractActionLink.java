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

package org.jppf.admin.web.tabletree;

import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.*;
import org.jppf.admin.web.*;
import org.jppf.utils.Pair;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractActionLink extends AjaxLink<String> {
  /**
   *
   */
  private transient UpdatableAction action;
  /**
   * 
   */
  protected String imageName;

  /**
   * @param id the link id.
   */
  public AbstractActionLink(final String id) {
    super(id);
  }

  /**
   * @param id the link id.
   * @param model the display model.
   */
  public AbstractActionLink(final String id, final IModel<String> model) {
    super(id, model);
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

  /**
   * Get the session from the ajax target.
   * @param target the ajax target.
   * @return a {@link JPPFWebSession} instance.
   */
  protected JPPFWebSession getSession(final AjaxRequestTarget target) {
    TemplatePage page = (TemplatePage) target.getPage();
    return page.getJPPFSession();
  }

  @Override
  protected void onComponentTag(final ComponentTag tag) {
    super.onComponentTag(tag);
    Pair<String, String> pair = getImageNameAndExtension();
    String format = "<img src='images/toolbar/%s.%s'/>";
    if ((action != null) && !action.isEnabled()) {
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
   * Decompose a file name into name + extension.
   * @return a Pair of name, extension.
   */
  protected Pair<String, String> getImageNameAndExtension() {
    if (imageName == null) return null;
    int idx = imageName.lastIndexOf('.');
    if (idx <= 0) return new Pair<>(imageName, null);
    return new Pair<>(imageName.substring(0, idx), imageName.substring(idx + 1));
  }

  /**
   * Stop the refresh timer, if any, on the page specified by the target.
   * @param target the Ajax target.
   */
  protected void stopRefreshTimer(final AjaxRequestTarget target) {
    if (target.getPage() instanceof TableTreeHolder) {
      AjaxSelfUpdatingTimerBehavior timer = ((TableTreeHolder) target.getPage()).getRefreshTimer();
      if (timer != null) timer.stop(null);
    }
  }

  /**
   * Restart the refresh timer, if any, on the page specified by the target.
   * @param target the Ajax target.
   */
  protected void restartRefreshTimer(final AjaxRequestTarget target) {
    if (target.getPage() instanceof TableTreeHolder) {
      AjaxSelfUpdatingTimerBehavior timer = ((TableTreeHolder) target.getPage()).getRefreshTimer();
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
}
