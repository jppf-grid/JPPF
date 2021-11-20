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

package org.jppf.utils;


/**
 * Implementation of a formatter that print the contents of a {@link java.util.Properties Properties} object as HTML.
 * @author Laurent Cohen
 */
public class HTMLPropertiesTableFormat extends PropertiesTableFormat {
  /**
   * 
   */
  private final boolean fullHtml;

  /**
   * Initialize this formatter with the specified title.
   * @param docTitle the title of the whole document.
   */
  public HTMLPropertiesTableFormat(final String docTitle) {
    this(docTitle, true);
  }

  /**
   * Initialize this formatter with the specified title.
   * @param docTitle the title of the whole document.
   * @param fullHtml .
   */
  public HTMLPropertiesTableFormat(final String docTitle, final boolean fullHtml) {
    super(docTitle);
    this.fullHtml = fullHtml;
  }

  /**
   * Write the prologue for the formatted text.
   */
  @Override
  public void start() {
    if (fullHtml) sb.append("<html><head></head><body style=\"font-family: Arial; font-size: 12pt\">");
    sb.append("<h1><font color=\"#2D3876\">").append(docTitle).append("</font></h1>");
  }

  /**
   * Write the epilogue for the formatted text.
   */
  @Override
  public void end() {
    if (fullHtml) sb.append("</body></html>");
  }

  /**
   * Write the prologue of a table.
   * @param title the title for the table.
   */
  @Override
  public void tableStart(final String title) {
    sb.append("<h2><font color=\"#2D3876\">").append(title).append("</font></h2>");
    sb.append("<table cellspacing=\"0\" cellpadding=\"1\" style=\"border: 0px\">");
  }

  /**
   * Write the prologue of a table.
   */
  @Override
  public void tableEnd() {
    sb.append("</table>");
  }

  /**
   * Write the prologue of a table row.
   */
  @Override
  public void rowStart() {
    sb.append("<tr>");
  }

  /**
   * Write the prologue of a table row.
   */
  @Override
  public void rowEnd() {
    sb.append("</tr>");
  }

  /**
   * Write the prologue of a table cell.
   */
  @Override
  public void cellStart() {
    sb.append("<td valign=\"top\">");
  }

  /**
   * Write the prologue of a table cell.
   */
  @Override
  public void cellEnd() {
    sb.append("</td>");
  }

  /**
   * Write the separator between 2 cells.
   */
  @Override
  public void cellSeparator() {
    //sb.append("<td width=\"5\"/>");
    sb.append("<td valign=\"top\"><font color=\"red\"> = </font></td>");
  }

  @Override
  public String formatName(final String name) {
    return "<font color=\"#2D3876\">" + name + "</font>";
  }
}
