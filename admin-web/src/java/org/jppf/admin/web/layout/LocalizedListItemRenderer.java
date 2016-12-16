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

package org.jppf.admin.web.layout;

import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.jppf.ui.monitoring.LocalizedListItem;

/**
 *
 * @author Laurent Cohen
 */
public class LocalizedListItemRenderer implements IChoiceRenderer<LocalizedListItem> {
  @Override
  public Object getDisplayValue(final LocalizedListItem item) {
    return item.label;
  }

  @Override
  public String getIdValue(final LocalizedListItem item, final int index) {
    return item.name;
  }

  @Override
  public LocalizedListItem getObject(final String id, final IModel<? extends List<? extends LocalizedListItem>> choices) {
    for (LocalizedListItem item: choices.getObject()) {
      if (id.equals(item.name)) return item;
    }
    return null;
  }
}
