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

package org.jppf.admin.web.stats;

import java.util.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.*;
import org.jppf.admin.web.*;
import org.jppf.ui.monitoring.data.Fields;
import org.jppf.utils.LocalizationUtils;

/**
 * A panel that associates a label with an icon.
 * @author Laurent Cohen
 */
public class StatisticsTablePanel extends Panel {
  /**
   * Localization base for the stats page.
   */
  private static final String STATS_BASE = "org.jppf.ui.i18n.StatsPage";
  /**
   * Localization base for the stats page.
   */
  private static final String FIELDS_BASE = "org.jppf.ui.i18n.StatFields";

  /**
   *
   * @param id id of this component.
   * @param name .
   * @param fields .
   */
  public StatisticsTablePanel(final String id, final String name, final Fields[] fields) {
    super(id);
    Locale locale = JPPFWebSession.get().getLocale();
    String caption = LocalizationUtils.getLocalized(STATS_BASE, name + ".label", locale);
    add(new Label("caption", (caption != null) ? caption : ""));
    List<IColumn<Fields, String>> columns = new ArrayList<>();
    columns.add(new StatsColumn(0));
    columns.add(new StatsColumn(1));
    DataTable<Fields, String> table = new DataTable<>("table", columns, new ListDataProvider<>(Arrays.asList(fields)), Long.MAX_VALUE);
    add(table);
  }

  /**
   * 
   */
  public class StatsColumn extends AbstractColumn<Fields, String> {
    /**
     * The column index.
     */
    private final int index;

    /**
     * Initialize this column.
     * @param index the column index.
     */
    public StatsColumn(final int index) {
      super(Model.of(""));
      this.index = index;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<Fields>> cellItem, final String componentId, final IModel<Fields> rowModel) {
      Fields field = rowModel.getObject();
      Locale locale = JPPFWebSession.get().getLocale();
      String value = null;
      if (index == 0) {
        value = LocalizationUtils.getLocalized(FIELDS_BASE, field.name(), locale);
      } else {
        StatsUpdater updater = JPPFWebConsoleApplication.get().getStatsUpdater();
        value = updater.formatLatestValue(locale, JPPFWebSession.get().getCurrentDriver(), field);
      }
      cellItem.add(new Label(componentId, value));
      cellItem.add(new AttributeModifier("class", index == 0 ? "stats_label" : "stats_number"));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case 1:
          return "stats_number";
      }
      return "stats_label";
    }
  }
}
