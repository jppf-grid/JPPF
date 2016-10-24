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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * Abstract super class for modal dialogs with ok and cancel buttons.
 * @author Laurent Cohen
 */
public abstract class AbstractModalForm extends Form<String> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractModalForm.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The prefix for the ids of all components.
   */
  protected final String prefix;

  /**
   * 
   * @param prefix the prefix for the ids of all components.
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public AbstractModalForm(final String prefix, final ModalWindow modal, final Runnable okAction) {
    super(prefix + ".form");
    this.prefix = prefix;
    createFields();
    AjaxButton okButton = new AjaxButton(prefix + ".ok") {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on {}.ok", AbstractModalForm.this.prefix);
        if (okAction != null) okAction.run();
        saveSettings();
        modal.close(target);
      }
    };
    add(okButton);
    setDefaultButton(okButton);
    add(new AjaxButton(prefix + ".cancel") {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on {}.cancel", AbstractModalForm.this.prefix);
        modal.close(target);
      }
    });
    loadSettings();
  }

  /**
   * Create the fields and add them to this form.
   */
  protected abstract void createFields();

  /**
   * Load the fields values.
   */
  protected final void loadSettings() {
    loadSettings(JPPFWebSession.get().getUserSettings().getProperties());
  }

  /**
   * Load the fields values from the specified properties.
   * @param props the properties to load from.
   */
  protected abstract void loadSettings(final TypedProperties props);

  /**
   * Save the fields values.
   */
  protected final void saveSettings() {
    UserSettings settings = JPPFWebSession.get().getUserSettings();
    saveSettings(settings.getProperties());
    settings.save();
  }

  /**
   * Save the fields values to the specified properties.
   * @param props the properties to save to.
   */
  protected abstract void saveSettings(final TypedProperties props);
}
