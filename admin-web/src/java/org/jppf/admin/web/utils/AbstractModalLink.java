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

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * @param <F> the type of form displayed in the modal window.
 * @author Laurent Cohen
 */
public abstract class AbstractModalLink<F extends AbstractModalForm> extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(AbstractModalLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The modal window opened upon click on the buttom.
   */
  protected transient ModalWindow modal;
  /**
   * The form displayed inside the modal window.
   */
  protected F modalForm;
  /**
   * The class of the page display inside the modal window.
   */
  protected Class<? extends Page> pageClass;

  /**
   * 
   * @param id id of this component.
   * @param model model of this component.
   * @param imageName name of the associated icon, if any.
   * @param pageClass class of the associated modal page.
   * @param form the form to which the modal window is added.
   */
  public AbstractModalLink(final String id, final IModel<String> model, final String imageName, final Class<? extends Page> pageClass, final Form<String> form) {
    super(id, model);
    this.imageName = imageName;
    this.pageClass = pageClass;
    modal = new ModalWindow(id + ".dialog");
    form.add(modal);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    modalForm = createForm();
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on {}", getDefaultModelObject());
    modal.setPageCreator(new ModalPageCreator<>(modalForm, pageClass));
    stopRefreshTimer(target);
    addTableTreeToTarget(target);
    modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
      @Override
      public void onClose(final AjaxRequestTarget target) {
        restartRefreshTimer(target);
      }
    });
    modal.show(target);
  }

  /**
   * Create a new form add to the page in the modal window.
   * @return a new form.
   */
  protected abstract F createForm();
}
