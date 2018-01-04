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

package org.jppf.admin.web.layout;

import java.util.*;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.utils.TypedProperties;


/**
 *
 * @author Laurent Cohen
 */
public class SelectableLayoutForm extends AbstractModalForm {
  /**
   * Text field for the number of threads.
   */
  private Palette<LocalizedListItem> paletteField;
  /**
   * The selectable layout page to get the items from.
   */
  private SelectableLayout layout;

  /**
   * @param layout the selectable layout page to get the items from.
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public SelectableLayoutForm(final SelectableLayout layout, final ModalWindow modal, final Runnable okAction) {
    super("selectable.layout", modal, okAction, layout);
    this.layout = layout;
  }

  @Override
  protected void createFields() {
    final List<LocalizedListItem> all = layout.getAllItems();
    paletteField = new Palette<>(prefix + ".palette.field", Model.ofList(layout.getVisibleItems()), Model.ofList(all), new LocalizedListItemRenderer(), all.size(), true);
    paletteField.add(new DefaultTheme());
    add(paletteField);
  }

  /**
   * @return the list of selected items.
   */
  public List<LocalizedListItem> getItems() {
    return new ArrayList<>(paletteField.getModelObject());
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
  }

  @Override
  protected boolean saveSettings(final TypedProperties props) {
    return false;
  }

  @Override
  protected void beforeCreateFields(final Object... args) {
    this.layout = (SelectableLayout) args[0];
  }
}
