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

package org.jppf.admin.web.utils;

import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.*;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.AbstractJPPFPage;
import org.jppf.utils.*;

/**
 * A form button that has an icon instead of text.
 * @author Laurent Cohen
 */
public class AjaxButtonWithIcon extends AjaxButton {
  /**
   * The fixed part of the button's style.
   */
  private static final String FIXED_STYLE = new StringBuilder(";background-repeat:no-repeat")
    .append(";background-position:4px").append(";background-attachment:scroll")
    .append(";padding:4px 12px").append(";margin-right:5px").toString();
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
  public AjaxButtonWithIcon(final String id) {
    this(id, null, null);
  }

  /**
   * @param id the link id.
   * @param model the display model.
   */
  public AjaxButtonWithIcon(final String id, final IModel<String> model) {
    this(id, model, null);
  }

  /**
   * @param id the link id.
   * @param imageName the name of the associated icon.
   */
  public AjaxButtonWithIcon(final String id, final String imageName) {
    this(id, null, imageName);
    this.imageName = imageName;
  }

  /**
   * @param id the link id.
   * @param model the display model.
   * @param imageName the name of the associated icon.
   */
  public AjaxButtonWithIcon(final String id, final IModel<String> model, final String imageName) {
    super(id, model);
    this.imageName = imageName;
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

  @Override
  protected void onComponentTag(final ComponentTag tag) {
    super.onComponentTag(tag);
    Pair<String, String> pair = FileUtils.getFileNameAndExtension(imageName);
    StringBuilder style = new StringBuilder();
    String format = "background-image: url(" + RequestCycle.get().getRequest().getContextPath() + "/images/toolbar/%s.%s)";
    if ((action != null) && (!action.isEnabled() || !action.isAuthorized())) {
      tag.getAttributes().put("class", "button_link_disabled");
      if (pair != null) style.append(String.format(format, pair.first() + "-disabled", pair.second()));
    } else {
      if (pair != null) style.append(String.format(format, pair.first(), pair.second()));
    }
    tag.getAttributes().put("style", style.append(FIXED_STYLE).toString());
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    if (tooltip) ((AbstractJPPFPage) getPage()).setTooltip(this);
  }
}
