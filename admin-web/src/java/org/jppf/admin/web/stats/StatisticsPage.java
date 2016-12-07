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

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.jppf.admin.web.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.monitoring.data.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * This is the admin page. It can only be instantiated for users with a {@code jppf-admin} role.
 * @author Laurent Cohen
 */
@MountPath("statistics")
public class StatisticsPage extends TemplatePage {
  /**
   * The behavior that periodically refreshes the toolbar and table tree.
   */
  protected final transient AjaxSelfUpdatingTimerBehavior refreshTimer = new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5));
  /**
   * Container for all the visible statitics tables.
   */
  private WebMarkupContainer tablesContainer;

  /**
   *
   */
  public StatisticsPage() {
    Form<String> form = new Form<>("stats.toolbar");
    add(form);
    List<TopologyDriver> drivers = JPPFWebConsoleApplication.get().getTopologyManager().getDrivers();
    DropDownChoice<TopologyDriver> combo = new DropDownChoice<TopologyDriver>("stats.driver_selector.field", drivers, new TopologyDriverRenderer()) {
      @Override
      protected void onSelectionChanged(final TopologyDriver newSelection) {
        JPPFWebSession.get().setCurrentDriver(newSelection);
        setResponsePage(StatisticsPage.class);
      }

      @Override
      protected boolean wantOnSelectionChangedNotifications() {
        return true;
      }
    };
    TopologyDriver driver = JPPFWebSession.get().getCurrentDriver();
    if (driver != null) combo.setModel(Model.of(driver));
    form.add(combo);
    form.add(new ServerResetStatsLink());
    form.add(new ExportLink(ExportLink.TEXT));
    form.add(new ExportLink(ExportLink.CSV));
    tablesContainer = new WebMarkupContainer("stats.tables.container");
    List<StatsTableData> tables = new ArrayList<>();
    for (Map.Entry<String, Fields[]> entry: StatsConstants.ALL_TABLES_MAP.entrySet()) tables.add(new StatsTableData(entry.getKey(), entry.getValue()));
    ListView<StatsTableData> listView = new ListView<StatsTableData>("stats.visible.tables", tables) {
      @Override
      protected void populateItem(final ListItem<StatsTableData> item) {
        item.add(new StatisticsTablePanel("stats.table", item.getModelObject().name, item.getModelObject().fields));
      }
    };
    tablesContainer.add(refreshTimer);
    tablesContainer.add(listView);
    add(tablesContainer);
  }

  /**
   * @return the container for all the visible statitics tables.
   */
  public WebMarkupContainer getTablesContainer() {
    return tablesContainer;
  }

  /**
   * Holds the needed information for each statitstics table.
   */
  private static class StatsTableData {
    /**
     * Name of the table.
     */
    public final String name;
    /**
     * List of fields in the table.
     */
    public final Fields[] fields;

    /**
     * @param name the name of the table.
     * @param fields the list of fields in the table.
     */
    public StatsTableData(final String name, final Fields[] fields) {
      this.name = name;
      this.fields = fields;
    }
  }
}
