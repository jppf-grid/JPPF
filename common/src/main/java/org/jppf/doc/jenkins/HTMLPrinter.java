/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.doc.jenkins;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class HTMLPrinter {
  /**
   * Format of the displayed dates.
   */
  static final SimpleDateFormat SDF = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
  /**
   * Indent level.
   */
  private int indentLevel = 0;
  /**
   * Indent string corresponding to the indent level.
   */
  private String indentString = "";

  /**
   * Generate the HTML source for the specified project.
   * @param project the project top explore.
   * @return an html representation of the project.
   */
  public String generate(final Project project) {
    final StringBuilder sb = new StringBuilder();
    sb.append(indent()).append("<div class=\"blockWithHighlightedTitle\" align='center'>\n");
    sb.append(incIndent()).append("<table><tr><td align='left'>\n");
    sb.append(indent()).append(String.format("<h2><img src='images/icons/monitoring.png' class='titleWithIcon'/>%s</h2>", project.getName())).append('\n');
    sb.append(incIndent()).append("<table cellpadding='3px' cellspacing='0'>\n");
    sb.append(incIndent()).append("<tr>\n");
    final String style1a = "border: 1px solid #6D78B6; border-right: 0px";
    final String style1b = "border: 1px solid #6D78B6;";
    final String style2a = "border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;";
    final String style2b = "border: 1px solid #6D78B6; border-top: 0px;";
    sb.append(incIndent()).append("<th align='center' valign='top' style='" + style1a + "'>Build #</th>\n");
    sb.append(indent()).append("<th align='center' valign='top' style='" + style1a + "'>Start</th>\n");
    sb.append(indent()).append("<th align='center' valign='top' style='" + style1a + "'>Duration</th>\n");
    sb.append(indent()).append("<th align='center' style='" + style1b + "'>Tests</th>\n");
    sb.append(decIndent()).append("</tr>\n");
    for (final Build build: project.getBuilds()) {
      final TestResults res = build.getTestResults();
      sb.append(indent()).append("<tr>\n");
      final String icon = "https://www.jppf.org/images/icons/" + ("SUCCESS".equals(build.getResult()) ? "default.png" : "bug1.png");
      sb.append(incIndent()).append("<td align='left' valign='bottom' style='" + style2a + "'>").append("<img width='16' height='16' src='" + icon + "'/> ").append(build.getNumber()).append("</td>\n");
      sb.append(indent()).append("<td align='right' valign='bottom' style='" + style2a + "'>").append(SDF.format(new Date(build.getStartTime()))).append("</td>\n");
      sb.append(indent()).append("<td align='right' valign='bottom' style='" + style2a + "'>").append(StringUtils.toStringDuration(build.getDuration())).append("</td>\n");
      sb.append(indent()).append("<td align='right' valign='bottom' style='" + style2b + "'>");
      if (res == null) sb.append("N/A");
      else sb.append(String.format("%,4d / %,4d / %,4d", res.getTotalCount(), res.getFailures(), res.getSkipped()));
      sb.append("</td>\n");
      sb.append(decIndent()).append("</tr>\n");
    }
    sb.append(decIndent()).append("</table>\n");
    sb.append(decIndent()).append("</td></tr></table>");
    sb.append(decIndent()).append("<br></div><br>\n");
    return sb.toString();
  }
  /**
   * Increment the indent level and update the indent accordingly.
   * @return the new indent string.
   */
  private String incIndent() {
    indentLevel++;
    return createIndent();
  }

  /**
   * Decrement the indent level and update the indent accordingly.
   * @return the new indent string.
   */
  private String decIndent() {
    indentLevel--;
    return createIndent();
  }

  /**
   * Get the current indent string.
   * @return the indent.
   */
  private String indent() {
    return indentString;
  }

  /**
   * Cretae the new indent string based on the indent level.
   * @return the new indent string.
   */
  private String createIndent() {
    if (indentLevel <= 0) indentString = "";
    else {
      final StringBuilder sb = new StringBuilder();
      for (int i=0; i<indentLevel; i++) sb.append("  ");
      indentString = sb.toString();
    }
    return indentString;
  }
}
