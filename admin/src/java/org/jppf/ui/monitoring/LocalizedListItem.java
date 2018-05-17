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

package org.jppf.ui.monitoring;

import java.util.Locale;

import org.jppf.utils.LocalizationUtils;

/**
 * Instances of this class are the items displayed in the pick list.
 * @author Laurent Cohen
 */
public class LocalizedListItem {
  /**
   * The non-localized name of this item.
   */
  private final String name;
  /**
   * The localized name of this item.
   */
  private final String label;
  /**
   * The localized tooltip of this item.
   */
  private final String tooltip;
  /**
   * Unique index for this item.
   */
  private final int index;

  /**
   * Initialize this item.
   * @param name the non-localized name of this item.
   * @param index unique index for this item.
   * @param base base name for localization bundle lookups.
   * @param locale the locale to localize in.
   */
  public LocalizedListItem(final String name, final int index, final String base, final Locale locale) {
    this.name = name;
    this.index = index;
    this.label = LocalizationUtils.getLocalized(base, name + ".label", locale);
    this.tooltip = LocalizationUtils.getLocalized(base, name + ".tooltip", locale);
  }

  /**
   * Initialize this item.
   * @param name the non-localized name of this item.
   * @param index unique index for this item.
   * @param label a displayable short label describing the field.
   * @param tooltip a tooltip associated with the field.
   */
  public LocalizedListItem(final String name, final int index, final String label, final String tooltip) {
    this.name = name;
    this.index = index;
    this.label = label;
    this.tooltip = tooltip;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  /**
   * @return the unique index for this item.
   */
  public int getIndex() {
    return index;
  }

  /**
   * @return the non-localized name of this item.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the localized name of this item.
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return the localized tooltip of this item.
   */
  public String getTooltip() {
    return tooltip;
  }
}
