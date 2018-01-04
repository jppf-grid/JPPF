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

package org.jppf.admin.web.admin;

import java.io.*;
import java.util.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents the save configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class SortLink extends AbstractAdminLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SortLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether to perform a sort in ascending ({@code true}) or descending ({@code false}) order.
   */
  private final boolean ascending;

  /**
   * Initialize.
   * @param type the type of config panel to add this button to.
   * @param ascending whether to perform a sort in ascending ({@code true}) or descending ({@code false}) order.
   */
  public SortLink(final ConfigType type, final boolean ascending) {
    super(type, ascending ? AdminConfigConstants.SORT_ASC_ACTION : AdminConfigConstants.SORT_DESC_ACTION, ascending ? "sort-ascending.png" : "sort-descending.png");
    this.ascending = ascending;
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
    if (debugEnabled) log.debug("clicked on {}.sort[{}]", type.getPrefix(), ((ascending ? "a" : "de") + "scending"));
    final TextArea<String> area = ((AdminPage) target.getPage()).getConfigPanel(type).getConfig();
    final String configString = area.getModelObject();
    try {
      final List<String> list = FileUtils.textFileAsLines(new StringReader(configString));
      Collections.sort(list);
      if (!ascending) Collections.reverse(list);
      final StringBuilder sb = new StringBuilder();
      for (final String s: list) sb.append(s).append('\n');
      area.setModel(Model.of(sb.toString()));
      target.add(form);
    } catch (final IOException e) {
      log.error(e.getMessage(), e);
    }
  }
}
