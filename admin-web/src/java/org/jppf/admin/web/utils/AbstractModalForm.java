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

import java.util.Locale;

import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.utils.*;
import org.slf4j.*;

import com.googlecode.wicket.jquery.ui.form.spinner.Spinner;

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
   */
  protected final Locale locale;

  /**
   *
   * @param prefix the prefix for the ids of all components.
   * @param modal the modal window.
   * @param okAction the ok action.
   * @param args optional arguments.
   */
  public AbstractModalForm(final String prefix, final ModalWindow modal, final Runnable okAction, final Object...args) {
    this(prefix, modal, okAction, true, args);
  }

  /**
   *
   * @param prefix the prefix for the ids of all components.
   * @param modal the modal window.
   * @param args optional arguments.
   */
  public AbstractModalForm(final String prefix, final ModalWindow modal, final Object...args) {
    this(prefix, modal, null, false, args);
  }

  /**
   *
   * @param prefix the prefix for the ids of all components.
   * @param modal the modal window.
   * @param okAction the ok action.
   * @param addDefaultButtons whether to add default ok and cancel buttons.
   * @param args optional arguments.
   */
  public AbstractModalForm(final String prefix, final ModalWindow modal, final Runnable okAction, final boolean addDefaultButtons, final Object...args) {
    super(prefix + ".form");
    this.prefix = prefix;
    this.locale = JPPFWebSession.get().getLocale();
    beforeCreateFields(args);
    createFields();
    if (addDefaultButtons) {
      final AjaxButton okButton = new AjaxButton(prefix + ".ok") {
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
    }
    loadSettings();
  }

  /**
   * @param args optional arguments.
   */
  protected void beforeCreateFields(@SuppressWarnings("unused") final Object...args) {
  }

  /**
   * Create the fields and add them to this form.
   */
  protected abstract void createFields();

  /**
   * Load the fields values.
   */
  protected final void loadSettings() {
    if (JPPFWebSession.get() != null) {
      final UserSettings settings = JPPFWebSession.get().getUserSettings();
      if (settings != null) loadSettings(settings.getProperties());
    }
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
    final UserSettings settings = JPPFWebSession.get().getUserSettings();
    if (saveSettings(settings.getProperties())) settings.save();
  }

  /**
   * Save the fields values to the specified properties.
   * @param props the properties to save to.
   * @return {@code true} if settings have been modified and must saved, {@code false} otherwise.
   */
  protected abstract boolean saveSettings(final TypedProperties props);

  /**
   * Create a spinner field for long values.
   * @param id id of the field.
   * @param value the current value.
   * @param min the minimum allowed value.
   * @param max the maximum allowed value.
   * @param step the amount to increment by.
   * @return the newly created text field.
   */
  protected TextField<Long> createLongField(final String id, final long value, final long min, final long max, final long step) {
    final Spinner<Long> spinner = new Spinner<>(id, Model.of(value), Long.class);
    spinner.setMin(min);
    spinner.setMax(max);
    spinner.setStep(step);
    spinner.setRequired(false);
    return spinner;
  }

  /**
   * Create a spinner field for int values.
   * @param id id of the field.
   * @param value the current value.
   * @param min the minimum allowed value.
   * @param max the maximum allowed value.
   * @param step the amount to increment by.
   * @return the newly created text field.
   */
  protected TextField<Integer> createIntField(final String id, final int value, final int min, final int max, final int step) {
    final Spinner<Integer> spinner = new Spinner<>(id, Model.of(value), Integer.class);
    spinner.setMin(min);
    spinner.setMax(max);
    spinner.setStep(step);
    spinner.setRequired(false);
    return spinner;
  }

  /**
   * Create a spinner field for decimal values.
   * @param id id of the field.
   * @param value the current value.
   * @param min the minimum allowed value.
   * @param max the maximum allowed value.
   * @param step the amount to increment by.
   * @return the newly created text field.
   */
  protected TextField<Double> createDecField(final String id, final double value, final double min, final double max, final double step) {
    final Spinner<Double> spinner = new Spinner<>(id, Model.of(value), Double.class);
    spinner.setMin(min);
    spinner.setMax(max);
    spinner.setStep(step);
    spinner.setRequired(false);
    return spinner;
  }

  /**
   * Add a tooltip to the specified component.
   * @param <T> the type of the component.
   * @param comp the component on which to set a tooltip.
   * @param base the base name for localization resource bundle lookup.
   * @return the component itself.
   */
  protected <T extends Component> T setTooltip(final T comp, final String base) {
    final String id = comp.getId();
    String key = null;
    if (id.endsWith(".field")) {
      final int idx = id.lastIndexOf(".field");
      key = id.substring(0, idx) + ".tooltip";
    } else key = id + ".tooltip";
    comp.add(new AttributeModifier("title", LocalizationUtils.getLocalized(base, key, locale)));
    return comp;
  }
}
